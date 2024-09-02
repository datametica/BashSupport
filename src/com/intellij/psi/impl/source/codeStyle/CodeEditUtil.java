/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.psi.impl.source.codeStyle;

import com.intellij.lang.*;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.impl.source.tree.TreeUtil;
import com.intellij.psi.templateLanguages.OuterLanguageElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.NotNullFunction;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CodeEditUtil {
  private static final Key<Boolean> GENERATED_FLAG = new Key<Boolean>("GENERATED_FLAG");
  private static final Key<Integer> INDENT_INFO = new Key<Integer>("INDENT_INFO");
  private static final Key<Boolean> REFORMAT_BEFORE_KEY = new Key<Boolean>("REFORMAT_BEFORE_KEY");
  private static final Key<Boolean> REFORMAT_KEY = new Key<Boolean>("REFORMAT_KEY");
  private static final ThreadLocal<Boolean> ALLOW_TO_MARK_NODES_TO_REFORMAT = new ThreadLocal<Boolean>() {
    @Override
    protected Boolean initialValue() {
      return Boolean.TRUE;
    }
  };
  private static final ThreadLocal<Boolean> ALLOW_NODES_REFORMATTING = new ThreadLocal<Boolean>() {
    @Override
    protected Boolean initialValue() {
      return Boolean.TRUE;
    }
  };
  private static final ThreadLocal<NotNullFunction<ASTNode, Boolean>> NODE_REFORMAT_STRATEGY = new ThreadLocal<NotNullFunction<ASTNode, Boolean>>();

  public static final Key<Boolean> OUTER_OK = new Key<Boolean>("OUTER_OK");

  private CodeEditUtil() { }

  public static void removeChild(ASTNode parent, @NotNull ASTNode child) {
    removeChildren(parent, child, child);
  }

  public static ASTNode addChildren(ASTNode parent, @NotNull ASTNode first, @NotNull ASTNode last, ASTNode anchorBefore) {
    ASTNode lastChild = last.getTreeNext();
    ASTNode current = first;
    while (current != lastChild) {
      saveWhitespacesInfo(current);
      checkForOuters(current);
      current = current.getTreeNext();
    }

    if (anchorBefore != null && isComment(anchorBefore.getElementType())) {
      final ASTNode anchorPrev = anchorBefore.getTreePrev();
      if (anchorPrev != null && anchorPrev.getElementType() == TokenType.WHITE_SPACE) {
        anchorBefore = anchorPrev;
      }
    }

    parent.addChildren(first, lastChild, anchorBefore);
    ASTNode firstAddedLeaf = findFirstLeaf(first, last);
    ASTNode prevLeaf = TreeUtil.prevLeaf(first);
    ASTNode result = first;
    if (firstAddedLeaf != null) {
      ASTNode placeHolderEnd = makePlaceHolderBetweenTokens(prevLeaf, firstAddedLeaf, isFormattingRequired(prevLeaf, first), false);
      if (placeHolderEnd != prevLeaf && first == firstAddedLeaf) {
        result = placeHolderEnd;
      }
      ASTNode lastAddedLeaf = findLastLeaf(first, last);
      placeHolderEnd = makePlaceHolderBetweenTokens(lastAddedLeaf, TreeUtil.nextLeaf(last), true, false);
      if (placeHolderEnd != lastAddedLeaf && lastAddedLeaf == first) {
        result = placeHolderEnd;
      }
    }
    else {
      makePlaceHolderBetweenTokens(prevLeaf, TreeUtil.nextLeaf(last), isFormattingRequired(prevLeaf, first), false);
    }
    return result;
  }

  private static boolean isComment(IElementType type) {
    final ParserDefinition def = LanguageParserDefinitions.INSTANCE.forLanguage(type.getLanguage());
    return def != null && def.getCommentTokens().contains(type);
  }

  private static boolean isFormattingRequired(ASTNode prevLeaf, ASTNode first) {
    while (first != null) {
      ASTNode current = prevLeaf;
      while (current != null) {
        if (current.getTreeNext() == first) return true;
        current = current.getTreeParent();
      }
      final ASTNode parent = first.getTreeParent();
      if (parent != null && parent.getTextRange().equals(first.getTextRange())) {
        first = parent;
      }
      else {
        break;
      }
    }
    return false;
  }

  public static void checkForOuters(ASTNode element) {
    if (element instanceof OuterLanguageElement && element.getCopyableUserData(OUTER_OK) == null) {
      throw new IllegalArgumentException("Outer element " + element + " is not allowed here");
    }

    ASTNode child = element.getFirstChildNode();
    while (child != null) {
      checkForOuters(child);
      child = child.getTreeNext();
    }
  }

  public static void saveWhitespacesInfo(ASTNode first) {
    if (first == null || isNodeGenerated(first) || getOldIndentation(first) >= 0) {
      return;
    }

    PsiElement psiElement = first.getPsi();
    if (psiElement == null) {
      return;
    }

    PsiFile file = psiElement.getContainingFile();
    //setOldIndentation((TreeElement)first, IndentHelper.getInstance().getIndent(file.getProject(), file.getFileType(), first));
  }

  public static int getOldIndentation(ASTNode node) {
    if (node == null) return -1;
    final Integer stored = node.getCopyableUserData(INDENT_INFO);
    return stored != null ? stored : -1;
  }

  public static void removeChildren(ASTNode parent, @NotNull ASTNode first, @NotNull ASTNode last) {
    final boolean tailingElement = last.getStartOffset() + last.getTextLength() == parent.getStartOffset() + parent.getTextLength();
    final boolean forceReformat = needToForceReformat(parent, first, last);
    saveWhitespacesInfo(first);

    TreeElement child = (TreeElement)first;
    while (child != null) {
      //checkForOuters(child);
      if (child == last) break;
      child = child.getTreeNext();
    }
    assert child == last : last + " is not a successor of " + first + " in the .getTreeNext() chain";

    final ASTNode prevLeaf = TreeUtil.prevLeaf(first);
    final ASTNode nextLeaf = TreeUtil.nextLeaf(first);
    parent.removeRange(first, last.getTreeNext());
    ASTNode nextLeafToAdjust = nextLeaf;
    if (nextLeafToAdjust != null && prevLeaf != null && nextLeafToAdjust.getTreeParent() == null) {
      //next element has invalidated
      nextLeafToAdjust = prevLeaf.getTreeNext();
    }
    makePlaceHolderBetweenTokens(prevLeaf, nextLeafToAdjust, forceReformat, tailingElement);
  }

  private static boolean needToForceReformat(final ASTNode parent, final ASTNode first, final ASTNode last) {
    return parent == null || first.getStartOffset() != parent.getStartOffset() ||
           parent.getText().trim().length() == getTrimmedTextLength(first, last) &&
           needToForceReformat(parent.getTreeParent(), parent, parent);
  }

  private static int getTrimmedTextLength(ASTNode first, final ASTNode last) {
    final StringBuilder buffer = new StringBuilder();
    while (first != last.getTreeNext()) {
      buffer.append(first.getText());
      first = first.getTreeNext();
    }
    return buffer.toString().trim().length();
  }

  public static void replaceChild(ASTNode parent, @NotNull ASTNode oldChild, @NotNull ASTNode newChild) {
    saveWhitespacesInfo(oldChild);
    saveWhitespacesInfo(newChild);
    checkForOuters(oldChild);
    checkForOuters(newChild);

    LeafElement oldFirst = TreeUtil.findFirstLeaf(oldChild);

    parent.replaceChild(oldChild, newChild);
    final LeafElement firstLeaf = TreeUtil.findFirstLeaf(newChild);
    final ASTNode prevToken = TreeUtil.prevLeaf(newChild);
    if (firstLeaf != null) {
      final ASTNode nextLeaf = TreeUtil.nextLeaf(newChild);
      makePlaceHolderBetweenTokens(prevToken, firstLeaf, isFormattingRequired(prevToken, newChild), false);
      if (nextLeaf != null && !CharArrayUtil.containLineBreaks(nextLeaf.getText())) {
        makePlaceHolderBetweenTokens(TreeUtil.prevLeaf(nextLeaf), nextLeaf, false, false);
      }
    }
    else {
      if (oldFirst != null && prevToken == null) {
        ASTNode whitespaceNode = newChild.getTreeNext();
        if (whitespaceNode != null && whitespaceNode.getElementType() == TokenType.WHITE_SPACE) {
          // Replacing non-empty prefix to empty shall remove whitespace
          parent.removeChild(whitespaceNode);
        }
      }

      makePlaceHolderBetweenTokens(prevToken, TreeUtil.nextLeaf(newChild), isFormattingRequired(prevToken, newChild), false);
    }
  }

  @Nullable
  private static ASTNode findFirstLeaf(ASTNode first, ASTNode last) {
    do {
      final LeafElement leaf = TreeUtil.findFirstLeaf(first);
      if (leaf != null) return leaf;
      first = first.getTreeNext();
      if (first == null) return null;
    }
    while (first != last);
    return null;
  }

  @Nullable
  private static ASTNode findLastLeaf(ASTNode first, ASTNode last) {
    do {
      final ASTNode leaf = TreeUtil.findLastLeaf(last);
      if (leaf != null) return leaf;
      last = last.getTreePrev();
      if (last == null) return null;
    }
    while (first != last);
    return null;
  }

  @Nullable
  private static ASTNode makePlaceHolderBetweenTokens(ASTNode left, ASTNode right, boolean forceReformat, boolean normalizeTrailingWS) {
    if (right == null) return left;

    markToReformatBefore(right, false);
    if (left == null) {
      markToReformatBefore(right, true);
    }
    else if (left.getElementType() == TokenType.WHITE_SPACE && left.getTreeNext() == null && normalizeTrailingWS) {
      // handle tailing whitespaces if element on the left has been removed
      final ASTNode prevLeaf = TreeUtil.prevLeaf(left);
      left.getTreeParent().removeChild(left);
      markToReformatBeforeOrInsertWhitespace(prevLeaf, right);
      left = right;
    }
    else if (left.getElementType() == TokenType.WHITE_SPACE && right.getElementType() == TokenType.WHITE_SPACE) {
      final String text;
      final int leftBlankLines = getBlankLines(left.getText());
      final int rightBlankLines = getBlankLines(right.getText());
      final boolean leaveRightText = leftBlankLines < rightBlankLines;
      if (leftBlankLines == 0 && rightBlankLines == 0) {
        text = left.getText() + right.getText();
      }
      else if (leaveRightText) {
        text = right.getText();
      }
      else {
        text = left.getText();
      }
      if (leaveRightText || forceReformat) {
        final LeafElement merged = ASTFactory.whitespace(text);
        if (!leaveRightText) {
          left.getTreeParent().replaceChild(left, merged);
          right.getTreeParent().removeChild(right);
        }
        else {
          right.getTreeParent().replaceChild(right, merged);
          left.getTreeParent().removeChild(left);
        }
        left = merged;
      }
      else {
        right.getTreeParent().removeChild(right);
      }
    }
    else if (left.getElementType() != TokenType.WHITE_SPACE || forceReformat) {
      if (right.getElementType() == TokenType.WHITE_SPACE) {
        markWhitespaceForReformat(right);
      }
      else if (left.getElementType() == TokenType.WHITE_SPACE) {
        markWhitespaceForReformat(left);
      }
      else {
        markToReformatBeforeOrInsertWhitespace(left, right);
      }
    }
    return left;
  }

  private static void markWhitespaceForReformat(final ASTNode right) {
    final String text = right.getText();
    final LeafElement merged = ASTFactory.whitespace(text);
    right.getTreeParent().replaceChild(right, merged);
  }

  private static void markToReformatBeforeOrInsertWhitespace(final ASTNode left, @NotNull final ASTNode right) {
    final Language leftLang = left != null ? PsiUtilCore.getNotAnyLanguage(left) : null;
    final Language rightLang = PsiUtilCore.getNotAnyLanguage(right);

    ASTNode generatedWhitespace = null;
    if (leftLang != null && leftLang.isKindOf(rightLang)) {
      //generatedWhitespace = LanguageTokenSeparatorGenerators.INSTANCE.forLanguage(leftLang).generateWhitespaceBetweenTokens(left, right);
    }
    else if (rightLang.isKindOf(leftLang)) {
      //generatedWhitespace = LanguageTokenSeparatorGenerators.INSTANCE.forLanguage(rightLang).generateWhitespaceBetweenTokens(left, right);
    }

    if (generatedWhitespace != null) {
      final TreeUtil.CommonParentState parentState = new TreeUtil.CommonParentState();
      TreeUtil.prevLeaf((TreeElement)right, parentState);
      parentState.nextLeafBranchStart.getTreeParent().addChild(generatedWhitespace, parentState.nextLeafBranchStart);
    }
    else {
      markToReformatBefore(right, true);
    }
  }

  public static void markToReformatBefore(final ASTNode right, boolean value) {
    right.putCopyableUserData(REFORMAT_BEFORE_KEY, value ? true : null);
  }

  private static int getBlankLines(final String text) {
    int result = 0;
    int currentIndex = -1;
    while ((currentIndex = text.indexOf('\n', currentIndex + 1)) >= 0) result++;
    return result;
  }

  public static boolean isNodeGenerated(final ASTNode node) {
    return node == null || node.getCopyableUserData(GENERATED_FLAG) != null;
  }

  public static void setNodeGenerated(final ASTNode next, final boolean value) {
    if (next == null) return;
    next.putCopyableUserData(GENERATED_FLAG, value ? true : null);
  }

  public static void setOldIndentation(final TreeElement treeElement, final int oldIndentation) {
    if (treeElement == null) return;
    treeElement.putCopyableUserData(INDENT_INFO, oldIndentation >= 0 ? oldIndentation : null);
  }
}
