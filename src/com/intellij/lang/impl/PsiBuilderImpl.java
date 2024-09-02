/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.lang.impl;


import com.ansorgit.plugins.bash.lang.psi.impl.BashFileImpl;
import com.intellij.lang.*;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.impl.source.CharTableImpl;
import com.intellij.psi.impl.source.resolve.FileContextUtil;
import com.intellij.psi.impl.source.text.DiffLog;
import com.intellij.psi.impl.source.tree.Factory;
import com.intellij.psi.impl.source.tree.*;
import com.intellij.psi.text.BlockSupport;
import com.intellij.psi.tree.*;
import com.intellij.util.*;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Convertor;
import com.intellij.util.containers.LimitedPool;
import com.intellij.util.containers.Stack;
import com.intellij.util.diff.DiffTreeChangeBuilder;
import com.intellij.util.diff.FlyweightCapableTreeStructure;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PsiBuilderImpl extends UserDataHolderBase implements PsiBuilder {
    private static final Logger LOG = Logger.getInstance("#com.intellij.lang.impl.PsiBuilderImpl");
    public static final Key<TripleFunction<ASTNode, LighterASTNode, FlyweightCapableTreeStructure<LighterASTNode>, ThreeState>> CUSTOM_COMPARATOR = Key.create("CUSTOM_COMPARATOR");
    private static final Key<LazyParseableTokensCache> LAZY_PARSEABLE_TOKENS = Key.create("LAZY_PARSEABLE_TOKENS");
    private static TokenSet ourAnyLanguageWhitespaceTokens;
    private PsiFile myFile;
    private int[] myLexStarts;
    private IElementType[] myLexTypes;
    private int myCurrentLexeme;
    private final MyList myProduction;
    private final Lexer myLexer;
    private final TokenSet myWhitespaces;
    private TokenSet myComments;
    private CharTable myCharTable;
    private final CharSequence myText;
    private final CharSequence myLastCommittedText;
    private final char[] myTextArray;
    private boolean myDebugMode;
    private int myLexemeCount;
    private boolean myTokenTypeChecked;
    private ITokenTypeRemapper myRemapper;
    private WhitespaceSkippedCallback myWhitespaceSkippedCallback;
    private final ASTNode myOriginalTree;
    private final MyTreeStructure myParentLightTree;
    private final int myOffset;
    private Map<Key, Object> myUserData;
    private IElementType myCachedTokenType;
    private final LimitedPool<StartMarker> START_MARKERS;
    private final LimitedPool<DoneMarker> DONE_MARKERS;
    private static final ArrayFactory<IElementType> myElementTypeArrayFactory;
    @NonNls
    private static final String UNBALANCED_MESSAGE = "Unbalanced tree. Most probably caused by unbalanced markers. Try calling setDebugMode(true) against PsiBuilder passed to identify exact location of the problem";

    public static void registerWhitespaceToken(@NotNull IElementType type) {
        ourAnyLanguageWhitespaceTokens = TokenSet.orSet(new TokenSet[]{ourAnyLanguageWhitespaceTokens, TokenSet.create(new IElementType[]{type})});
    }

    public PsiBuilderImpl(PsiFile containingFile, @NotNull ParserDefinition parserDefinition, @NotNull Lexer lexer, CharTable charTable, @NotNull CharSequence text, @Nullable ASTNode originalTree, @Nullable MyTreeStructure parentLightTree) {
        this(containingFile, parserDefinition.getWhitespaceTokens(), parserDefinition.getCommentTokens(), lexer, charTable, text, originalTree, originalTree == null ? null : originalTree.getText(), parentLightTree, (Object)null);
    }

    public PsiBuilderImpl(PsiFile containingFile, @NotNull TokenSet whiteSpaces, @NotNull TokenSet comments, @NotNull Lexer lexer, CharTable charTable, @NotNull CharSequence text, @Nullable ASTNode originalTree, @Nullable MyTreeStructure parentLightTree) {
        this(containingFile, whiteSpaces, comments, lexer, charTable, text, originalTree, originalTree == null ? null : originalTree.getText(), parentLightTree, (Object)null);
    }

    private PsiBuilderImpl(PsiFile containingFile, @NotNull TokenSet whiteSpaces, @NotNull TokenSet comments, @NotNull Lexer lexer, CharTable charTable, @NotNull CharSequence text, @Nullable ASTNode originalTree, @Nullable CharSequence lastCommittedText, @Nullable MyTreeStructure parentLightTree, @Nullable Object parentCachingNode) {
        this.myProduction = new MyList();
        this.START_MARKERS = new LimitedPool(2000, new LimitedPool.ObjectFactory<StartMarker>() {
            @NotNull
            public StartMarker create() {
                return new StartMarker();
            }

            public void cleanup(@NotNull StartMarker startMarker) {
                startMarker.clean();
            }
        });
        this.DONE_MARKERS = new LimitedPool(2000, new LimitedPool.ObjectFactory<DoneMarker>() {
            @NotNull
            public DoneMarker create() {
                return new DoneMarker();
            }

            public void cleanup(@NotNull DoneMarker doneMarker) {
                doneMarker.clean();
            }
        });
        this.myFile = containingFile;
        this.myText = text;
        this.myTextArray = CharArrayUtil.fromSequenceWithoutCopying(text);
        this.myLexer = lexer;
        this.myWhitespaces = whiteSpaces;
        this.myComments = comments;
        this.myCharTable = charTable;
        this.myOriginalTree = originalTree;
        this.myLastCommittedText = lastCommittedText;
        if (originalTree == null != (lastCommittedText == null)) {
            throw new IllegalArgumentException("originalTree and lastCommittedText must be null/notnull together but got: originalTree=" + originalTree + "; lastCommittedText=" + (lastCommittedText == null ? null : "'" + StringUtil.first(lastCommittedText, 80, true) + "'"));
        } else {
            this.myParentLightTree = parentLightTree;
            this.myOffset = parentCachingNode instanceof LazyParseableToken ? ((LazyParseableToken)parentCachingNode).getStartOffset() : 0;
            this.cacheLexemes(parentCachingNode);
        }
    }

    public PsiBuilderImpl(@NotNull ParserDefinition parserDefinition, @NotNull Lexer lexer, @NotNull ASTNode chameleon, @NotNull CharSequence text) {
        //String a = "";

        this(SharedImplUtil.getContainingFile(chameleon), parserDefinition.getWhitespaceTokens(), parserDefinition.getCommentTokens(), lexer, SharedImplUtil.findCharTableByTree(chameleon), text, (ASTNode)Pair.getFirst((Pair)chameleon.getUserData(BlockSupport.TREE_TO_BE_REPARSED)), (CharSequence)Pair.getSecond((Pair)chameleon.getUserData(BlockSupport.TREE_TO_BE_REPARSED)), (MyTreeStructure)null, chameleon);
    }

    public PsiBuilderImpl(BashFileImpl impl, @NotNull ParserDefinition parserDefinition, @NotNull Lexer lexer, @NotNull ASTNode chameleon, @NotNull CharSequence text) {
        this(
                impl,
                parserDefinition.getWhitespaceTokens(),
                parserDefinition.getCommentTokens(),
                lexer,
                SharedImplUtil.findCharTableByTree(chameleon),
                text,
                null/*(ASTNode)Pair.getFirst((Pair)chameleon.getUserData(BlockSupport.TREE_TO_BE_REPARSED))*/,
                null/*(CharSequence)Pair.getSecond((Pair)chameleon.getUserData(BlockSupport.TREE_TO_BE_REPARSED))*/,
                (MyTreeStructure)null,
                null
        );
    }

    public PsiBuilderImpl(@NotNull ParserDefinition parserDefinition, @NotNull Lexer lexer, @NotNull LighterLazyParseableNode chameleon, @NotNull CharSequence text) {
        this(chameleon.getContainingFile(), parserDefinition.getWhitespaceTokens(), parserDefinition.getCommentTokens(), lexer, chameleon.getCharTable(), text, (ASTNode)null, (CharSequence)null, ((LazyParseableToken)chameleon).myParent, chameleon);
    }

    private void cacheLexemes(@Nullable Object parentCachingNode) {
        int[] lexStarts = null;
        IElementType[] lexTypes = null;
        int lexemeCount = -1;
        boolean doLexingOptimizationCorrectionCheck = false;
        int i;
        int offset;
        int j;
        if (parentCachingNode instanceof LazyParseableToken) {
            LazyParseableToken parentToken = (LazyParseableToken)parentCachingNode;
            i = parentToken.myEndIndex - parentToken.myStartIndex;
            if (i != 1) {
                lexStarts = new int[i + 1];
                System.arraycopy(parentToken.myBuilder.myLexStarts, parentToken.myStartIndex, lexStarts, 0, i);
                offset = parentToken.myBuilder.myLexStarts[parentToken.myStartIndex];

                for(j = 0; j < i; ++j) {
                    lexStarts[j] -= offset;
                }

                lexStarts[i] = this.myText.length();
                lexTypes = new IElementType[i];
                System.arraycopy(parentToken.myBuilder.myLexTypes, parentToken.myStartIndex, lexTypes, 0, i);
                lexemeCount = i;
            }

            ProgressIndicatorProvider.checkCanceled();
            if (!doLexingOptimizationCorrectionCheck && lexemeCount != -1) {
                this.myLexStarts = lexStarts;
                this.myLexTypes = lexTypes;
                this.myLexemeCount = lexemeCount;
                return;
            }
        } else if (parentCachingNode instanceof LazyParseableElement) {
            LazyParseableElement parentElement = (LazyParseableElement)parentCachingNode;
            LazyParseableTokensCache cachedTokens = (LazyParseableTokensCache)parentElement.getUserData(LAZY_PARSEABLE_TOKENS);
            parentElement.putUserData(LAZY_PARSEABLE_TOKENS, null);
            if (!doLexingOptimizationCorrectionCheck && cachedTokens != null) {
                this.myLexStarts = cachedTokens.myLexStarts;
                this.myLexTypes = cachedTokens.myLexTypes;
                this.myLexemeCount = this.myLexTypes.length;
                return;
            }
        }

        int approxLexCount = Math.max(10, this.myText.length() / 5);
        this.myLexStarts = new int[approxLexCount];
        this.myLexTypes = new IElementType[approxLexCount];
        this.myLexer.start(this.myText);
        i = 0;
        offset = 0;

        while(true) {
            //ProgressIndicatorProvider.checkCanceled();
            IElementType type = this.myLexer.getTokenType();
            if (type == null) {
                this.myLexStarts[i] = this.myText.length();
                this.myLexemeCount = i;
                this.clearCachedTokenType();
                if (doLexingOptimizationCorrectionCheck && lexemeCount != -1) {
                    assert lexemeCount == this.myLexemeCount;

                    for(j = 0; j < lexemeCount; ++j) {
                        assert this.myLexStarts[j] == lexStarts[j] && this.myLexTypes[j] == lexTypes[j];
                    }

                    assert this.myLexStarts[lexemeCount] == lexStarts[lexemeCount];
                }

                return;
            }

            if (i >= this.myLexTypes.length - 1) {
                this.resizeLexemes(i * 3 / 2);
            }

            int tokenStart = this.myLexer.getTokenStart();
            if (tokenStart < offset) {
                StringBuilder sb = new StringBuilder();
                IElementType tokenType = this.myLexer.getTokenType();
                sb.append("Token sequence broken").append("\n  this: '").append(this.myLexer.getTokenText()).append("' (").append(tokenType).append(':').append(tokenType != null ? tokenType.getLanguage() : null).append(") ").append(tokenStart).append(":").append(this.myLexer.getTokenEnd());
                int quoteStart;
                if (i > 0) {
                    quoteStart = this.myLexStarts[i - 1];
                    sb.append("\n  prev: '").append(this.myText.subSequence(quoteStart, offset)).append("' (").append(this.myLexTypes[i - 1]).append(':').append(this.myLexTypes[i - 1].getLanguage()).append(") ").append(quoteStart).append(":").append(offset);
                }

                quoteStart = Math.max(tokenStart - 256, 0);
                int quoteEnd = Math.min(tokenStart + 256, this.myText.length());
                sb.append("\n  quote: [").append(quoteStart).append(':').append(quoteEnd).append("] '").append(this.myText.subSequence(quoteStart, quoteEnd)).append('\'');
                LOG.error(sb);
            }

            offset = tokenStart;
            this.myLexStarts[i] = tokenStart;
            this.myLexTypes[i] = type;
            ++i;
            this.myLexer.advance();
        }
    }

    public void enforceCommentTokens(@NotNull TokenSet tokens) {
        this.myComments = tokens;
    }

    @Nullable
    public LighterASTNode getLatestDoneMarker() {
        for(int index = this.myProduction.size() - 1; index >= 0; --index) {
            ProductionMarker marker = (ProductionMarker)this.myProduction.get(index);
            if (marker instanceof DoneMarker) {
                return ((DoneMarker)marker).myStart;
            }
        }

        return null;
    }

    @NotNull
    private PsiBuilder.Marker precede(StartMarker marker) {
        int idx = this.myProduction.lastIndexOf(marker);
        if (idx < 0) {
            LOG.error("Cannot precede dropped or rolled-back marker");
        }

        StartMarker pre = this.createMarker(marker.myLexemeIndex);
        this.myProduction.add(idx, pre);
        return pre;
    }

    public CharSequence getOriginalText() {
        return this.myText;
    }

    @Nullable
    public IElementType getTokenType() {
        IElementType cached = this.myCachedTokenType;
        if (cached == null) {
            this.myCachedTokenType = cached = this.calcTokenType();
        }

        return cached;
    }

    private void clearCachedTokenType() {
        this.myCachedTokenType = null;
    }

    private IElementType remapCurrentToken() {
        if (this.myCachedTokenType != null) {
            return this.myCachedTokenType;
        } else {
            if (this.myRemapper != null) {
                this.remapCurrentToken(this.myRemapper.filter(this.myLexTypes[this.myCurrentLexeme], this.myLexStarts[this.myCurrentLexeme], this.myLexStarts[this.myCurrentLexeme + 1], this.myLexer.getBufferSequence()));
            }

            return this.myLexTypes[this.myCurrentLexeme];
        }
    }

    private IElementType calcTokenType() {
        if (this.eof()) {
            return null;
        } else {
            if (this.myRemapper != null) {
                this.skipWhitespace();
            }

            return this.myLexTypes[this.myCurrentLexeme];
        }
    }

    public void setTokenTypeRemapper(ITokenTypeRemapper remapper) {
        this.myRemapper = remapper;
        this.myTokenTypeChecked = false;
        this.clearCachedTokenType();
    }

    public void remapCurrentToken(IElementType type) {
        this.myLexTypes[this.myCurrentLexeme] = type;
        this.clearCachedTokenType();
    }

    @Nullable
    public IElementType lookAhead(int steps) {
        if (this.eof()) {
            return null;
        } else {
            int cur;
            for(cur = this.myCurrentLexeme; steps > 0; --steps) {
                ++cur;

                while(cur < this.myLexemeCount && this.whitespaceOrComment(this.myLexTypes[cur])) {
                    ++cur;
                }
            }

            return cur < this.myLexemeCount ? this.myLexTypes[cur] : null;
        }
    }

    public IElementType rawLookup(int steps) {
        int cur = this.myCurrentLexeme + steps;
        return cur < this.myLexemeCount && cur >= 0 ? this.myLexTypes[cur] : null;
    }

    public int rawTokenTypeStart(int steps) {
        int cur = this.myCurrentLexeme + steps;
        if (cur < 0) {
            return -1;
        } else {
            return cur >= this.myLexemeCount ? this.getOriginalText().length() : this.myLexStarts[cur];
        }
    }

    public int rawTokenIndex() {
        return this.myCurrentLexeme;
    }

    public int rawTokenOffset(int tokenIndex) {
        return this.myLexStarts[tokenIndex];
    }

    public void setWhitespaceSkippedCallback(@Nullable WhitespaceSkippedCallback callback) {
        this.myWhitespaceSkippedCallback = callback;
    }

    public void advanceLexer() {
        ProgressIndicatorProvider.checkCanceled();
        if (!this.eof()) {
            if (!this.myTokenTypeChecked) {
                LOG.error("Probably a bug: eating token without its type checking");
            }

            this.myTokenTypeChecked = false;
            ++this.myCurrentLexeme;
            this.clearCachedTokenType();
        }
    }

    private void skipWhitespace() {
        while(this.myCurrentLexeme < this.myLexemeCount && this.whitespaceOrComment(this.remapCurrentToken())) {
            this.onSkip(this.myLexTypes[this.myCurrentLexeme], this.myLexStarts[this.myCurrentLexeme], this.myCurrentLexeme + 1 < this.myLexemeCount ? this.myLexStarts[this.myCurrentLexeme + 1] : this.myText.length());
            ++this.myCurrentLexeme;
            this.clearCachedTokenType();
        }

    }

    private void onSkip(IElementType type, int start, int end) {
        if (this.myWhitespaceSkippedCallback != null) {
            this.myWhitespaceSkippedCallback.onSkip(type, start, end);
        }

    }

    public int getCurrentOffset() {
        return this.eof() ? this.getOriginalText().length() : this.myLexStarts[this.myCurrentLexeme];
    }

    @Nullable
    public String getTokenText() {
        if (this.eof()) {
            return null;
        } else {
            IElementType type = this.getTokenType();
            return type instanceof TokenWrapper ? ((TokenWrapper)type).getValue() : this.myText.subSequence(this.myLexStarts[this.myCurrentLexeme], this.myLexStarts[this.myCurrentLexeme + 1]).toString();
        }
    }

    private void resizeLexemes(int newSize) {
        this.myLexStarts = ArrayUtil.realloc(this.myLexStarts, newSize + 1);
        this.myLexTypes = (IElementType[])ArrayUtil.realloc(this.myLexTypes, newSize, myElementTypeArrayFactory);
        this.clearCachedTokenType();
    }

    public boolean whitespaceOrComment(IElementType token) {
        return this.myWhitespaces.contains(token) || this.myComments.contains(token);
    }

    @NotNull
    public PsiBuilder.Marker mark() {
        if (!this.myProduction.isEmpty()) {
            this.skipWhitespace();
        }

        StartMarker marker = this.createMarker(this.myCurrentLexeme);
        this.myProduction.add(marker);
        return marker;
    }

    @NotNull
    private StartMarker createMarker(int lexemeIndex) {
        StartMarker marker = (StartMarker)this.START_MARKERS.alloc();
        marker.myLexemeIndex = lexemeIndex;
        marker.myBuilder = this;
        if (this.myDebugMode) {
            marker.myDebugAllocationPosition = new Throwable("Created at the following trace.");
        }

        return marker;
    }

    public final boolean eof() {
        if (!this.myTokenTypeChecked) {
            this.myTokenTypeChecked = true;
            this.skipWhitespace();
        }

        return this.myCurrentLexeme >= this.myLexemeCount;
    }

    private void rollbackTo(@NotNull PsiBuilder.Marker marker) {
        this.myCurrentLexeme = ((StartMarker)marker).myLexemeIndex;
        this.myTokenTypeChecked = true;
        int idx = this.myProduction.lastIndexOf(marker);
        if (idx < 0) {
            LOG.error("The marker must be added before rolled back to.");
        }

        this.myProduction.removeRange(idx, this.myProduction.size());
        this.START_MARKERS.recycle((StartMarker)marker);
        this.clearCachedTokenType();
    }

    public boolean hasErrorsAfter(@NotNull PsiBuilder.Marker marker) {
        assert marker instanceof StartMarker;

        int idx = this.myProduction.lastIndexOf(marker);
        if (idx < 0) {
            LOG.error("The marker must be added before checked for errors.");
        }

        for(int i = idx + 1; i < this.myProduction.size(); ++i) {
            ProductionMarker m = (ProductionMarker)this.myProduction.get(i);
            if (m instanceof ErrorItem || m instanceof DoneWithErrorMarker) {
                return true;
            }
        }

        return false;
    }

    public void drop(@NotNull PsiBuilder.Marker marker) {
        DoneMarker doneMarker = ((StartMarker)marker).myDoneMarker;
        if (doneMarker != null) {
            this.myProduction.remove(this.myProduction.lastIndexOf(doneMarker));
            this.DONE_MARKERS.recycle(doneMarker);
        }

        boolean removed = this.myProduction.remove(this.myProduction.lastIndexOf(marker)) == marker;
        if (!removed) {
            LOG.error("The marker must be added before it is dropped.");
        }

        this.START_MARKERS.recycle((StartMarker)marker);
    }

    public void error(@NotNull PsiBuilder.Marker marker, String message) {
        this.doValidityChecks(marker, (Marker)null);
        DoneWithErrorMarker doneMarker = new DoneWithErrorMarker((StartMarker)marker, this.myCurrentLexeme, message);
        boolean tieToTheLeft = this.isEmpty(((StartMarker)marker).myLexemeIndex, this.myCurrentLexeme);
        if (tieToTheLeft) {
            ((StartMarker)marker).myEdgeTokenBinder = WhitespacesBinders.DEFAULT_RIGHT_BINDER;
        }

        ((StartMarker)marker).myDoneMarker = doneMarker;
        this.myProduction.add(doneMarker);
    }

    private void errorBefore(@NotNull PsiBuilder.Marker marker, String message, @NotNull PsiBuilder.Marker before) {
        this.doValidityChecks(marker, before);
        int beforeIndex = this.myProduction.lastIndexOf(before);
        DoneWithErrorMarker doneMarker = new DoneWithErrorMarker((StartMarker)marker, ((StartMarker)before).myLexemeIndex, message);
        boolean tieToTheLeft = this.isEmpty(((StartMarker)marker).myLexemeIndex, ((StartMarker)before).myLexemeIndex);
        if (tieToTheLeft) {
            ((StartMarker)marker).myEdgeTokenBinder = WhitespacesBinders.DEFAULT_RIGHT_BINDER;
        }

        ((StartMarker)marker).myDoneMarker = doneMarker;
        this.myProduction.add(beforeIndex, doneMarker);
    }

    public void done(@NotNull PsiBuilder.Marker marker) {
        this.doValidityChecks(marker, (Marker)null);
        DoneMarker doneMarker = (DoneMarker)this.DONE_MARKERS.alloc();
        doneMarker.myStart = (StartMarker)marker;
        doneMarker.myLexemeIndex = this.myCurrentLexeme;
        boolean tieToTheLeft = doneMarker.myStart.myType.isLeftBound() && this.isEmpty(((StartMarker)marker).myLexemeIndex, this.myCurrentLexeme);
        if (tieToTheLeft) {
            ((StartMarker)marker).myEdgeTokenBinder = WhitespacesBinders.DEFAULT_RIGHT_BINDER;
        }

        ((StartMarker)marker).myDoneMarker = doneMarker;
        this.myProduction.add(doneMarker);
    }

    public void doneBefore(@NotNull PsiBuilder.Marker marker, @NotNull PsiBuilder.Marker before) {
        this.doValidityChecks(marker, before);
        int beforeIndex = this.myProduction.lastIndexOf(before);
        DoneMarker doneMarker = (DoneMarker)this.DONE_MARKERS.alloc();
        doneMarker.myLexemeIndex = ((StartMarker)before).myLexemeIndex;
        doneMarker.myStart = (StartMarker)marker;
        boolean tieToTheLeft = doneMarker.myStart.myType.isLeftBound() && this.isEmpty(((StartMarker)marker).myLexemeIndex, ((StartMarker)before).myLexemeIndex);
        if (tieToTheLeft) {
            ((StartMarker)marker).myEdgeTokenBinder = WhitespacesBinders.DEFAULT_RIGHT_BINDER;
        }

        ((StartMarker)marker).myDoneMarker = doneMarker;
        this.myProduction.add(beforeIndex, doneMarker);
    }

    private boolean isEmpty(int startIdx, int endIdx) {
        for(int i = startIdx; i < endIdx; ++i) {
            IElementType token = this.myLexTypes[i];
            if (!this.whitespaceOrComment(token)) {
                return false;
            }
        }

        return true;
    }

    public void collapse(@NotNull PsiBuilder.Marker marker) {
        this.done(marker);
        ((StartMarker)marker).myDoneMarker.myCollapse = true;
    }

    private void doValidityChecks(@NotNull PsiBuilder.Marker marker, @Nullable PsiBuilder.Marker before) {
        DoneMarker doneMarker = ((StartMarker)marker).myDoneMarker;
        if (doneMarker != null) {
            LOG.error("Marker already done.");
        }

        if (this.myDebugMode) {
            int idx = this.myProduction.lastIndexOf(marker);
            if (idx < 0) {
                LOG.error("Marker has never been added.");
            }

            int endIdx = this.myProduction.size();
            if (before != null) {
                endIdx = this.myProduction.lastIndexOf(before);
                if (endIdx < 0) {
                    LOG.error("'Before' marker has never been added.");
                }

                if (idx > endIdx) {
                    LOG.error("'Before' marker precedes this one.");
                }
            }

            for(int i = endIdx - 1; i > idx; --i) {
                Object item = this.myProduction.get(i);
                if (item instanceof StartMarker) {
                    StartMarker otherMarker = (StartMarker)item;
                    if (otherMarker.myDoneMarker == null) {
                        Throwable debugAllocOther = otherMarker.myDebugAllocationPosition;
                        Throwable debugAllocThis = ((StartMarker)marker).myDebugAllocationPosition;
                        if (debugAllocOther != null) {
                            Throwable currentTrace = new Throwable();
                            ExceptionUtil.makeStackTraceRelative(debugAllocThis, currentTrace).printStackTrace(System.err);
                            ExceptionUtil.makeStackTraceRelative(debugAllocOther, currentTrace).printStackTrace(System.err);
                        }

                        LOG.error("Another not done marker added after this one. Must be done before this.");
                    }
                }
            }

        }
    }

    public void error(String messageText) {
        ProductionMarker lastMarker = (ProductionMarker)this.myProduction.get(this.myProduction.size() - 1);
        if (!(lastMarker instanceof ErrorItem) || lastMarker.myLexemeIndex != this.myCurrentLexeme) {
            this.myProduction.add(new ErrorItem(this, messageText, this.myCurrentLexeme));
        }
    }

    @NotNull
    public ASTNode getTreeBuilt() {
/*
        boolean var8 = false;

        ASTNode var1;
        try {
            var8 = true;
            var1 = this.buildTree();
            var8 = false;
        } finally {
            if (var8) {
                Iterator var5 = this.myProduction.iterator();

                while(true) {
                    if (!var5.hasNext()) {
                        ;
                    } else {
                        ProductionMarker marker = (ProductionMarker)var5.next();
                        if (marker instanceof StartMarker) {
                            this.START_MARKERS.recycle((StartMarker)marker);
                        } else if (marker instanceof DoneMarker) {
                            this.DONE_MARKERS.recycle((DoneMarker)marker);
                        }
                    }
                }
            }
        }

        Iterator var2 = this.myProduction.iterator();

        while(var2.hasNext()) {
            ProductionMarker marker = (ProductionMarker)var2.next();
            if (marker instanceof StartMarker) {
                this.START_MARKERS.recycle((StartMarker)marker);
            } else if (marker instanceof DoneMarker) {
                this.DONE_MARKERS.recycle((DoneMarker)marker);
            }
        }

        return var1;
*/
        try {
            return buildTree();
        }
        finally {
            for (ProductionMarker marker : myProduction) {
                if (marker instanceof StartMarker) {
                    START_MARKERS.recycle((StartMarker)marker);
                }
                else if (marker instanceof DoneMarker) {
                    DONE_MARKERS.recycle((DoneMarker)marker);
                }
            }
        }
    }

    @NotNull
    private ASTNode buildTree() {
        StartMarker rootMarker = this.prepareLightTree();
        boolean isTooDeep = this.myFile != null && BlockSupport.isTooDeep(this.myFile.getOriginalFile());
        if (this.myOriginalTree != null && !isTooDeep) {
            DiffLog diffLog = this.merge(this.myOriginalTree, rootMarker, this.myLastCommittedText);
            throw new BlockSupport.ReparsedSuccessfullyException(diffLog);
        } else {
            ASTNode rootNode = this.createRootAST(rootMarker);
            this.bind(rootMarker, (CompositeElement)rootNode);
            if (isTooDeep && !(rootNode instanceof FileElement)) {
                ASTNode childNode = rootNode.getFirstChildNode();
                childNode.putUserData(BlockSupport.TREE_DEPTH_LIMIT_EXCEEDED, Boolean.TRUE);
            }

            return rootNode;
        }
    }

    @NotNull
    public FlyweightCapableTreeStructure<LighterASTNode> getLightTree() {
        StartMarker rootMarker = this.prepareLightTree();
        return new MyTreeStructure(rootMarker, this.myParentLightTree);
    }

    @NotNull
    private ASTNode createRootAST(@NotNull StartMarker rootMarker) {
        IElementType type = rootMarker.getTokenType();
        ASTNode rootNode = type instanceof ILazyParseableElementType ? ASTFactory.lazy((ILazyParseableElementType)type, (CharSequence)null) : createComposite(rootMarker);
        if (this.myCharTable == null) {
            this.myCharTable = (CharTable)(rootNode instanceof FileElement ? ((FileElement)rootNode).getCharTable() : new CharTableImpl());
        }

        if (!(rootNode instanceof FileElement)) {
            ((ASTNode)rootNode).putUserData(CharTable.CHAR_TABLE_KEY, this.myCharTable);
        }

        return (ASTNode)rootNode;
    }

    @NotNull
    private DiffLog merge(@NotNull ASTNode oldRoot, @NotNull StartMarker newRoot, @NotNull CharSequence lastCommittedText) {
        DiffLog diffLog = new DiffLog();
        //DiffTreeChangeBuilder<ASTNode, LighterASTNode> builder = new ConvertFromTokensToASTBuilder(newRoot, diffLog);
        MyTreeStructure treeStructure = new MyTreeStructure(newRoot, (MyTreeStructure)null);
        //ShallowNodeComparator<ASTNode, LighterASTNode> comparator = new MyComparator((TripleFunction)this.getUserDataUnprotected(CUSTOM_COMPARATOR), treeStructure);
        ProgressIndicator indicator = ProgressIndicatorProvider.getGlobalProgressIndicator();
        //BlockSupportImpl.diffTrees(oldRoot, builder, comparator, treeStructure, (ProgressIndicator)(indicator == null ? new EmptyProgressIndicator() : indicator), lastCommittedText);
        return diffLog;
    }

    @NotNull
    private StartMarker prepareLightTree() {
        if (this.myProduction.isEmpty()) {
            LOG.error("Parser produced no markers. Text:\n" + this.myText);
        }

        StartMarker rootMarker = (StartMarker)this.myProduction.get(0);
        if (rootMarker.myFirstChild != null) {
            return rootMarker;
        } else {
            this.myTokenTypeChecked = true;
            this.balanceWhiteSpaces();
            rootMarker.myParent = rootMarker.myFirstChild = rootMarker.myLastChild = rootMarker.myNext = null;
            StartMarker curNode = rootMarker;
            Stack<StartMarker> nodes = ContainerUtil.newStack();
            nodes.push(rootMarker);
            int lastErrorIndex = -1;
            int maxDepth = 0;
            int curDepth = 0;

            for(int i = 1; i < this.myProduction.size(); ++i) {
                ProductionMarker item = (ProductionMarker)this.myProduction.get(i);
                if (curNode == null) {
                    LOG.error("Unexpected end of the production");
                }

                item.myParent = curNode;
                if (item instanceof StartMarker) {
                    StartMarker marker = (StartMarker)item;
                    marker.myFirstChild = marker.myLastChild = marker.myNext = null;
                    curNode.addChild(marker);
                    nodes.push(curNode);
                    curNode = marker;
                    ++curDepth;
                    if (curDepth > maxDepth) {
                        maxDepth = curDepth;
                    }
                } else if (item instanceof DoneMarker) {
                    this.assertMarkersBalanced(((DoneMarker)item).myStart == curNode, item);
                    curNode = (StartMarker)nodes.pop();
                    --curDepth;
                } else if (item instanceof ErrorItem) {
                    int curToken = item.myLexemeIndex;
                    if (curToken != lastErrorIndex) {
                        lastErrorIndex = curToken;
                        curNode.addChild(item);
                    }
                }
            }

            List missed;
            if (this.myCurrentLexeme < this.myLexemeCount) {
                missed = ContainerUtil.newArrayList(this.myLexTypes, this.myCurrentLexeme, this.myLexemeCount);
                LOG.error("Tokens " + missed + " were not inserted into the tree. " + (this.myFile != null ? this.myFile.getLanguage() + ", " : "") + "Text:\n" + this.myText);
            }

            if (rootMarker.myDoneMarker.myLexemeIndex < this.myLexemeCount) {
                missed = ContainerUtil.newArrayList(this.myLexTypes, rootMarker.myDoneMarker.myLexemeIndex, this.myLexemeCount);
                LOG.error("Tokens " + missed + " are outside of root element \"" + rootMarker.myType + "\". Text:\n" + this.myText);
            }

            if (this.myLexStarts.length <= this.myCurrentLexeme + 1) {
                this.resizeLexemes(this.myCurrentLexeme + 1);
            }

            this.myLexStarts[this.myCurrentLexeme] = this.myText.length();
            this.myLexStarts[this.myCurrentLexeme + 1] = 0;
            this.myLexTypes[this.myCurrentLexeme] = null;
            this.assertMarkersBalanced(curNode == rootMarker, curNode);
            this.checkTreeDepth(maxDepth, rootMarker.getTokenType() instanceof IFileElementType);
            this.clearCachedTokenType();
            return rootMarker;
        }
    }

    private void assertMarkersBalanced(boolean condition, @Nullable ProductionMarker marker) {
        if (!condition) {
            int index = marker != null ? marker.getStartIndex() + 1 : this.myLexStarts.length;
            CharSequence context = index < this.myLexStarts.length ? this.myText.subSequence(Math.max(0, this.myLexStarts[index] - 1000), this.myLexStarts[index]) : "<none>";
            String language = this.myFile != null ? this.myFile.getLanguage() + ", " : "";
            LOG.error("Unbalanced tree. Most probably caused by unbalanced markers. Try calling setDebugMode(true) against PsiBuilder passed to identify exact location of the problem\nlanguage: " + language + "\n" + "context: '" + context + "'");
        }
    }

    private void balanceWhiteSpaces() {
        RelativeTokenTypesView wsTokens = new RelativeTokenTypesView();
        RelativeTokenTextView tokenTextGetter = new RelativeTokenTextView();
        int lastIndex = 0;
        int i = 1;

        for(int size = this.myProduction.size() - 1; i < size; ++i) {
            ProductionMarker item = (ProductionMarker)this.myProduction.get(i);
            if (item instanceof StartMarker) {
                this.assertMarkersBalanced(((StartMarker)item).myDoneMarker != null, item);
            }

            boolean recursive = item.myEdgeTokenBinder instanceof WhitespacesAndCommentsBinder.RecursiveBinder;
            int prevProductionLexIndex = recursive ? 0 : ((ProductionMarker)this.myProduction.get(i - 1)).myLexemeIndex;

            int wsStartIndex;
            for(wsStartIndex = Math.max(item.myLexemeIndex, lastIndex); wsStartIndex > prevProductionLexIndex && this.whitespaceOrComment(this.myLexTypes[wsStartIndex - 1]); --wsStartIndex) {
            }

            int wsEndIndex;
            for(wsEndIndex = item.myLexemeIndex; wsEndIndex < this.myLexemeCount && this.whitespaceOrComment(this.myLexTypes[wsEndIndex]); ++wsEndIndex) {
            }

            if (wsStartIndex == wsEndIndex) {
                if (item.myLexemeIndex < wsStartIndex) {
                    item.myLexemeIndex = wsStartIndex;
                }
            } else {
                wsTokens.configure(wsStartIndex, wsEndIndex);
                tokenTextGetter.configure(wsStartIndex);
                boolean atEnd = wsStartIndex == 0 || wsEndIndex == this.myLexemeCount;
                item.myLexemeIndex = wsStartIndex + item.myEdgeTokenBinder.getEdgePosition(wsTokens, atEnd, tokenTextGetter);
                if (recursive) {
                    for(int k = i - 1; k > 1; --k) {
                        ProductionMarker prev = (ProductionMarker)this.myProduction.get(k);
                        if (prev.myLexemeIndex < item.myLexemeIndex) {
                            break;
                        }

                        prev.myLexemeIndex = item.myLexemeIndex;
                    }
                }
            }

            lastIndex = item.myLexemeIndex;
        }

    }

    private void checkTreeDepth(int maxDepth, boolean isFileRoot) {
        if (this.myFile != null) {
            PsiFile file = this.myFile.getOriginalFile();
            Boolean flag = (Boolean)file.getUserData(BlockSupport.TREE_DEPTH_LIMIT_EXCEEDED);
            if (maxDepth > BlockSupport.INCREMENTAL_REPARSE_DEPTH_LIMIT) {
                if (!Boolean.TRUE.equals(flag)) {
                    file.putUserData(BlockSupport.TREE_DEPTH_LIMIT_EXCEEDED, Boolean.TRUE);
                }
            } else if (isFileRoot && flag != null) {
                file.putUserData(BlockSupport.TREE_DEPTH_LIMIT_EXCEEDED, null);
            }

        }
    }

    private void bind(@NotNull StartMarker rootMarker, @NotNull CompositeElement rootNode) {
        StartMarker curMarker = rootMarker;
        CompositeElement curNode = rootNode;
        int lexIndex = rootMarker.myLexemeIndex;
        ProductionMarker item = rootMarker.myFirstChild != null ? rootMarker.myFirstChild : rootMarker.myDoneMarker;

        while(true) {
            while(true) {
                lexIndex = this.insertLeaves(lexIndex, ((ProductionMarker)item).myLexemeIndex, curNode);
                if (item == rootMarker.myDoneMarker) {
                    return;
                }

                if (item instanceof StartMarker) {
                    StartMarker marker = (StartMarker)item;
                    if (!marker.myDoneMarker.myCollapse) {
                        curMarker = marker;
                        CompositeElement childNode = createComposite(marker);
                        curNode.rawAddChildrenWithoutNotifications(childNode);
                        curNode = childNode;
                        item = marker.myFirstChild != null ? marker.myFirstChild : marker.myDoneMarker;
                        continue;
                    }

                    lexIndex = this.collapseLeaves(curNode, marker);
                } else if (item instanceof ErrorItem) {
                    CompositeElement errorElement = Factory.createErrorElement(((ErrorItem)item).myMessage);
                    curNode.rawAddChildrenWithoutNotifications(errorElement);
                } else if (item instanceof DoneMarker) {
                    curMarker = (StartMarker)((DoneMarker)item).myStart.myParent;
                    curNode = curNode.getTreeParent();
                    item = ((DoneMarker)item).myStart;
                }

                item = ((ProductionMarker)item).myNext != null ? ((ProductionMarker)item).myNext : curMarker.myDoneMarker;
            }
        }
    }

    private int insertLeaves(int curToken, int lastIdx, CompositeElement curNode) {
        for(lastIdx = Math.min(lastIdx, this.myLexemeCount); curToken < lastIdx; ++curToken) {
            ProgressIndicatorProvider.checkCanceled();
            int start = this.myLexStarts[curToken];
            int end = this.myLexStarts[curToken + 1];
            if (start < end || this.myLexTypes[curToken] instanceof ILeafElementType) {
                IElementType type = this.myLexTypes[curToken];
                TreeElement leaf = this.createLeaf(type, start, end);
                curNode.rawAddChildrenWithoutNotifications(leaf);
            }
        }

        return curToken;
    }

    private int collapseLeaves(@NotNull CompositeElement ast, @NotNull StartMarker startMarker) {
        int start = this.myLexStarts[startMarker.myLexemeIndex];
        int end = this.myLexStarts[startMarker.myDoneMarker.myLexemeIndex];
        IElementType markerType = startMarker.myType;
        TreeElement leaf = this.createLeaf(markerType, start, end);
        if (markerType instanceof ILazyParseableElementType && ((ILazyParseableElementType)markerType).reuseCollapsedTokens() && startMarker.myLexemeIndex < startMarker.myDoneMarker.myLexemeIndex) {
            int length = startMarker.myDoneMarker.myLexemeIndex - startMarker.myLexemeIndex;
            int[] relativeStarts = new int[length + 1];
            IElementType[] types = new IElementType[length];

            for(int i = startMarker.myLexemeIndex; i < startMarker.myDoneMarker.myLexemeIndex; ++i) {
                relativeStarts[i - startMarker.myLexemeIndex] = this.myLexStarts[i] - start;
                types[i - startMarker.myLexemeIndex] = this.myLexTypes[i];
            }

            relativeStarts[length] = end - start;
            leaf.putUserData(LAZY_PARSEABLE_TOKENS, new LazyParseableTokensCache(relativeStarts, types));
        }

        ast.rawAddChildrenWithoutNotifications(leaf);
        return startMarker.myDoneMarker.myLexemeIndex;
    }

    @NotNull
    private static CompositeElement createComposite(@NotNull StartMarker marker) {
        IElementType type = marker.myType;
        if (type == TokenType.ERROR_ELEMENT) {
            String message = marker.myDoneMarker instanceof DoneWithErrorMarker ? ((DoneWithErrorMarker)marker.myDoneMarker).myMessage : null;
            return Factory.createErrorElement(message);
        } else if (type == null) {
            throw new RuntimeException("Unbalanced tree. Most probably caused by unbalanced markers. Try calling setDebugMode(true) against PsiBuilder passed to identify exact location of the problem");
        } else {
            return ASTFactory.composite(type);
        }
    }

    @Nullable
    public static String getErrorMessage(@NotNull LighterASTNode node) {
        if (node instanceof ErrorItem) {
            return ((ErrorItem)node).myMessage;
        } else {
            if (node instanceof StartMarker) {
                StartMarker marker = (StartMarker)node;
                if (marker.myType == TokenType.ERROR_ELEMENT && marker.myDoneMarker instanceof DoneWithErrorMarker) {
                    return ((DoneWithErrorMarker)marker.myDoneMarker).myMessage;
                }
            }

            return null;
        }
    }

    public void setDebugMode(boolean dbgMode) {
        this.myDebugMode = dbgMode;
    }

    @NotNull
    public Lexer getLexer() {
        return this.myLexer;
    }

    @NotNull
    private TreeElement createLeaf(@NotNull IElementType type, int start, int end) {
        CharSequence text = this.myCharTable.intern(this.myText, start, end);
        if (this.myWhitespaces.contains(type)) {
            return new PsiWhiteSpaceImpl(text);
        } else if (type instanceof CustomParsingType) {
            return (TreeElement)((CustomParsingType)type).parse(text, this.myCharTable);
        } else {
            return (TreeElement)(type instanceof ILazyParseableElementType ? ASTFactory.lazy((ILazyParseableElementType)type, text) : ASTFactory.leaf(type, text));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getUserDataUnprotected(@NotNull Key<T> key) {
        if (key == FileContextUtil.CONTAINING_FILE_KEY) {
            return (T) this.myFile;
        } else {
            return this.myUserData != null ? (T) this.myUserData.get(key) : null;
        }
    }

    public <T> void putUserDataUnprotected(@NotNull Key<T> key, @Nullable T value) {
        if (key == FileContextUtil.CONTAINING_FILE_KEY) {
            this.myFile = (PsiFile)value;
        } else {
            if (this.myUserData == null) {
                this.myUserData = ContainerUtil.newHashMap();
            }

            this.myUserData.put(key, value);
        }
    }

    static {
        ourAnyLanguageWhitespaceTokens = TokenSet.EMPTY;
        myElementTypeArrayFactory = new ArrayFactory<IElementType>() {
            @NotNull
            public IElementType[] create(int count) {
                return count == 0 ? IElementType.EMPTY_ARRAY : new IElementType[count];
            }
        };
    }

    private static class LazyParseableTokensCache {
        final int[] myLexStarts;
        final IElementType[] myLexTypes;

        public LazyParseableTokensCache(int[] lexStarts, IElementType[] lexTypes) {
            this.myLexStarts = lexStarts;
            this.myLexTypes = lexTypes;
        }
    }

    private static class MyList extends ArrayList<ProductionMarker> {
        private static final Field ourElementDataField = ReflectionUtil.getDeclaredField(ArrayList.class, "elementData");
        private Object[] cachedElementData;

        protected void removeRange(int fromIndex, int toIndex) {
            super.removeRange(fromIndex, toIndex);
        }

        private MyList() {
            super(256);
        }

        public int lastIndexOf(Object o) {
            Object[] data = this.cachedElementData;
            if (data == null) {
                return super.lastIndexOf(o);
            } else {
                for(int i = this.size() - 1; i >= 0; --i) {
                    if (data[i] == o) {
                        return i;
                    }
                }

                return -1;
            }
        }

        public void ensureCapacity(int minCapacity) {
            if (this.cachedElementData == null || minCapacity >= this.cachedElementData.length) {
                super.ensureCapacity(minCapacity);
                this.initCachedField();
            }

        }

        private void initCachedField() {
            if (ourElementDataField != null) {
                try {
                    this.cachedElementData = (Object[])((Object[])ourElementDataField.get(this));
                } catch (Exception var2) {
                    Exception e = var2;
                    PsiBuilderImpl.LOG.error(e);
                }

            }
        }
    }

    private static class ASTConverter implements Convertor<Node, ASTNode> {
        @NotNull
        private final StartMarker myRoot;

        private ASTConverter(@NotNull StartMarker root) {
            this.myRoot = root;
        }

        public ASTNode convert(Node n) {
            if (n instanceof Token) {
                Token token = (Token)n;
                return token.myBuilder.createLeaf(token.getTokenType(), token.myTokenStart, token.myTokenEnd);
            } else if (n instanceof ErrorItem) {
                return Factory.createErrorElement(((ErrorItem)n).myMessage);
            } else {
                StartMarker startMarker = (StartMarker)n;
                CompositeElement composite = n == this.myRoot ? (CompositeElement)this.myRoot.myBuilder.createRootAST(this.myRoot) : PsiBuilderImpl.createComposite(startMarker);
                startMarker.myBuilder.bind(startMarker, composite);
                return composite;
            }
        }
    }

    private static class MyTreeStructure implements FlyweightCapableTreeStructure<LighterASTNode> {
        private final LimitedPool<Token> myPool;
        private final LimitedPool<LazyParseableToken> myLazyPool;
        private final StartMarker myRoot;
        private int count;
        private LighterASTNode[] nodes;

        public MyTreeStructure(@NotNull StartMarker root, @Nullable MyTreeStructure parentTree) {
            if (parentTree == null) {
                this.myPool = new LimitedPool(1000, new LimitedPool.ObjectFactory<Token>() {
                    public void cleanup(@NotNull Token token) {
                        token.clean();
                    }

                    @NotNull
                    public Token create() {
                        return new TokenNode();
                    }
                });
                this.myLazyPool = new LimitedPool(200, new LimitedPool.ObjectFactory<LazyParseableToken>() {
                    public void cleanup(@NotNull LazyParseableToken token) {
                        token.clean();
                    }

                    @NotNull
                    public LazyParseableToken create() {
                        return new LazyParseableToken();
                    }
                });
            } else {
                this.myPool = parentTree.myPool;
                this.myLazyPool = parentTree.myLazyPool;
            }

            this.myRoot = root;
        }

        @NotNull
        public LighterASTNode getRoot() {
            return this.myRoot;
        }

        public LighterASTNode getParent(@NotNull LighterASTNode node) {
            if (node instanceof ProductionMarker) {
                return ((ProductionMarker)node).myParent;
            } else {
                throw new UnsupportedOperationException("Unknown node type: " + node);
            }
        }

        @NotNull
        public LighterASTNode prepareForGetChildren(@NotNull LighterASTNode node) {
            return node;
        }

        public int getChildren(@NotNull LighterASTNode item, @NotNull Ref<LighterASTNode[]> into) {
            if (item instanceof LazyParseableToken) {
                FlyweightCapableTreeStructure<LighterASTNode> tree = ((LazyParseableToken)item).parseContents();
                LighterASTNode root = (LighterASTNode)tree.getRoot();
                return tree.getChildren(tree.prepareForGetChildren(root), into);
            } else if (!(item instanceof Token) && !(item instanceof ErrorItem)) {
                StartMarker marker = (StartMarker)item;
                this.count = 0;
                ProductionMarker child = marker.myFirstChild;

                int lexIndex;
                for(lexIndex = marker.myLexemeIndex; child != null; child = child.myNext) {
                    lexIndex = this.insertLeaves(lexIndex, child.myLexemeIndex, marker.myBuilder);
                    if (child instanceof StartMarker && ((StartMarker)child).myDoneMarker.myCollapse) {
                        int lastIndex = ((StartMarker)child).myDoneMarker.myLexemeIndex;
                        this.insertLeaf(child.getTokenType(), marker.myBuilder, child.myLexemeIndex, lastIndex, true);
                    } else {
                        this.ensureCapacity();
                        this.nodes[this.count++] = child;
                    }

                    if (child instanceof StartMarker) {
                        lexIndex = ((StartMarker)child).myDoneMarker.myLexemeIndex;
                    }
                }

                this.insertLeaves(lexIndex, marker.myDoneMarker.myLexemeIndex, marker.myBuilder);
                into.set(this.nodes == null ? LighterASTNode.EMPTY_ARRAY : this.nodes);
                this.nodes = null;
                return this.count;
            } else {
                return 0;
            }
        }

        public void disposeChildren(LighterASTNode[] nodes, int count) {
            if (nodes != null) {
                for(int i = 0; i < count; ++i) {
                    LighterASTNode node = nodes[i];
                    if (node instanceof LazyParseableToken) {
                        this.myLazyPool.recycle((LazyParseableToken)node);
                    } else if (node instanceof Token) {
                        this.myPool.recycle((Token)node);
                    }
                }

            }
        }

        private void ensureCapacity() {
            LighterASTNode[] old = this.nodes;
            if (old == null) {
                old = new LighterASTNode[10];
                this.nodes = old;
            } else if (this.count >= old.length) {
                LighterASTNode[] newStore = new LighterASTNode[this.count * 3 / 2];
                System.arraycopy(old, 0, newStore, 0, this.count);
                this.nodes = newStore;
            }

        }

        private int insertLeaves(int curToken, int lastIdx, PsiBuilderImpl builder) {
            for(lastIdx = Math.min(lastIdx, builder.myLexemeCount); curToken < lastIdx; ++curToken) {
                this.insertLeaf(builder.myLexTypes[curToken], builder, curToken, curToken + 1, false);
            }

            return curToken;
        }

        private void insertLeaf(@NotNull IElementType type, @NotNull PsiBuilderImpl builder, int startLexemeIndex, int endLexemeIndex, boolean forceInsertion) {
            int start = builder.myLexStarts[startLexemeIndex];
            int end = builder.myLexStarts[endLexemeIndex];
            if (start <= end && (forceInsertion || start != end || type instanceof ILeafElementType)) {
                Token lexeme;
                if (type instanceof ILightLazyParseableElementType) {
                    lexeme = (Token)this.myLazyPool.alloc();
                    LazyParseableToken lazyParseableToken = (LazyParseableToken)lexeme;
                    lazyParseableToken.myParent = this;
                    lazyParseableToken.myStartIndex = startLexemeIndex;
                    lazyParseableToken.myEndIndex = endLexemeIndex;
                } else {
                    lexeme = (Token)this.myPool.alloc();
                }

                lexeme.myBuilder = builder;
                lexeme.myTokenType = type;
                lexeme.myTokenStart = start;
                lexeme.myTokenEnd = end;
                this.ensureCapacity();
                this.nodes[this.count++] = lexeme;
            }
        }

        @NotNull
        public CharSequence toString(@NotNull LighterASTNode node) {
            return this.myRoot.myBuilder.myText.subSequence(node.getStartOffset(), node.getEndOffset());
        }

        public int getStartOffset(@NotNull LighterASTNode node) {
            return node.getStartOffset();
        }

        public int getEndOffset(@NotNull LighterASTNode node) {
            return node.getEndOffset();
        }
    }

    private static class MyComparator /*implements ShallowNodeComparator<ASTNode, LighterASTNode>*/ {
        private final TripleFunction<ASTNode, LighterASTNode, FlyweightCapableTreeStructure<LighterASTNode>, ThreeState> custom;
        private final MyTreeStructure myTreeStructure;

        private MyComparator(TripleFunction<ASTNode, LighterASTNode, FlyweightCapableTreeStructure<LighterASTNode>, ThreeState> custom, @NotNull MyTreeStructure treeStructure) {
            this.custom = custom;
            this.myTreeStructure = treeStructure;
        }

        @NotNull
        public ThreeState deepEqual(@NotNull ASTNode oldNode, @NotNull LighterASTNode newNode) {
            ProgressIndicatorProvider.checkCanceled();
            boolean oldIsErrorElement = oldNode instanceof PsiErrorElement;
            boolean newIsErrorElement = newNode.getTokenType() == TokenType.ERROR_ELEMENT;
            if (oldIsErrorElement != newIsErrorElement) {
                return ThreeState.NO;
            } else if (oldIsErrorElement) {
                PsiErrorElement e1 = (PsiErrorElement)oldNode;
                return Comparing.equal(e1.getErrorDescription(), PsiBuilderImpl.getErrorMessage(newNode)) ? ThreeState.UNSURE : ThreeState.NO;
            } else {
                if (this.custom != null) {
                    ThreeState customResult = (ThreeState)this.custom.fun(oldNode, newNode, this.myTreeStructure);
                    if (customResult != ThreeState.UNSURE) {
                        return customResult;
                    }
                }

                if (newNode instanceof Token) {
                    IElementType type = newNode.getTokenType();
                    Token token = (Token)newNode;
                    if (oldNode instanceof ForeignLeafPsiElement) {
                        return type instanceof ForeignLeafType && ((ForeignLeafType)type).getValue().equals(oldNode.getText()) ? ThreeState.YES : ThreeState.NO;
                    }

                    if (oldNode instanceof LeafElement) {
                        if (type instanceof ForeignLeafType) {
                            return ThreeState.NO;
                        }

                        return ((LeafElement)oldNode).textMatches(token.getText()) ? ThreeState.YES : ThreeState.NO;
                    }

                    if (type instanceof ILightLazyParseableElementType) {
                        return ((TreeElement)oldNode).textMatches(token.getText()) ? ThreeState.YES : (TreeUtil.isCollapsedChameleon(oldNode) ? ThreeState.NO : ThreeState.UNSURE);
                    }

                    if (oldNode.getElementType() instanceof ILazyParseableElementType && type instanceof ILazyParseableElementType || oldNode.getElementType() instanceof CustomParsingType && type instanceof CustomParsingType) {
                        return ((TreeElement)oldNode).textMatches(token.getText()) ? ThreeState.YES : ThreeState.NO;
                    }
                }

                return ThreeState.UNSURE;
            }
        }

        public boolean typesEqual(@NotNull ASTNode n1, @NotNull LighterASTNode n2) {
            if (!(n1 instanceof PsiWhiteSpaceImpl)) {
                Object n1t;
                IElementType n2t;
                if (n1 instanceof ForeignLeafPsiElement) {
                    n1t = ((ForeignLeafPsiElement)n1).getForeignType();
                    n2t = n2.getTokenType();
                } else {
                    n1t = dereferenceToken(n1.getElementType());
                    n2t = dereferenceToken(n2.getTokenType());
                }

                return Comparing.equal(n1t, n2t);
            } else {
                return PsiBuilderImpl.ourAnyLanguageWhitespaceTokens.contains(n2.getTokenType()) || n2 instanceof Token && ((Token)n2).myBuilder.myWhitespaces.contains(n2.getTokenType());
            }
        }

        private static IElementType dereferenceToken(IElementType probablyWrapper) {
            return probablyWrapper instanceof TokenWrapper ? dereferenceToken(((TokenWrapper)probablyWrapper).getDelegate()) : probablyWrapper;
        }

        public boolean hashCodesEqual(@NotNull ASTNode n1, @NotNull LighterASTNode n2) {
            if (n1 instanceof LeafElement && n2 instanceof Token) {
                boolean isForeign1 = n1 instanceof ForeignLeafPsiElement;
                boolean isForeign2 = n2.getTokenType() instanceof ForeignLeafType;
                if (isForeign1 != isForeign2) {
                    return false;
                } else {
                    return isForeign1 ? n1.getText().equals(((ForeignLeafType)n2.getTokenType()).getValue()) : ((LeafElement)n1).textMatches(((Token)n2).getText());
                }
            } else {
                if (n1 instanceof PsiErrorElement && n2.getTokenType() == TokenType.ERROR_ELEMENT) {
                    PsiErrorElement e1 = (PsiErrorElement)n1;
                    if (!Comparing.equal(e1.getErrorDescription(), PsiBuilderImpl.getErrorMessage(n2))) {
                        return false;
                    }
                }

                return ((TreeElement)n1).hc() == ((Node)n2).hc();
            }
        }
    }

    private final class RelativeTokenTextView implements WhitespacesAndCommentsBinder.TokenTextGetter {
        private int myStart;

        private RelativeTokenTextView() {
        }

        private void configure(int start) {
            this.myStart = start;
        }

        @NotNull
        public CharSequence get(int i) {
            return PsiBuilderImpl.this.myText.subSequence(PsiBuilderImpl.this.myLexStarts[this.myStart + i], PsiBuilderImpl.this.myLexStarts[this.myStart + i + 1]);
        }
    }

    private final class RelativeTokenTypesView extends AbstractList<IElementType> {
        private int myStart;
        private int mySize;

        private RelativeTokenTypesView() {
        }

        private void configure(int start, int end) {
            this.myStart = start;
            this.mySize = end - start;
        }

        public IElementType get(int index) {
            return PsiBuilderImpl.this.myLexTypes[this.myStart + index];
        }

        public int size() {
            return this.mySize;
        }
    }

    private static class ConvertFromTokensToASTBuilder implements DiffTreeChangeBuilder<ASTNode, LighterASTNode> {
        private final DiffTreeChangeBuilder<ASTNode, ASTNode> myDelegate;
        private final ASTConverter myConverter;

        private ConvertFromTokensToASTBuilder(@NotNull StartMarker rootNode, @NotNull DiffTreeChangeBuilder<ASTNode, ASTNode> delegate) {
            this.myDelegate = delegate;
            this.myConverter = new ASTConverter(rootNode);
        }

        public void nodeDeleted(@NotNull ASTNode oldParent, @NotNull ASTNode oldNode) {
            this.myDelegate.nodeDeleted(oldParent, oldNode);
        }

        public void nodeInserted(@NotNull ASTNode oldParent, @NotNull LighterASTNode newNode, int pos) {
            this.myDelegate.nodeInserted(oldParent, this.myConverter.convert((Node)newNode), pos);
        }

        public void nodeReplaced(@NotNull ASTNode oldChild, @NotNull LighterASTNode newChild) {
            ASTNode converted = this.myConverter.convert((Node)newChild);
            this.myDelegate.nodeReplaced(oldChild, converted);
        }
    }

    private static class ErrorItem extends ProductionMarker {
        private final PsiBuilderImpl myBuilder;
        private String myMessage;

        public ErrorItem(PsiBuilderImpl builder, String message, int idx) {
            this.myBuilder = builder;
            this.myMessage = message;
            this.myLexemeIndex = idx;
            this.myEdgeTokenBinder = WhitespacesBinders.DEFAULT_RIGHT_BINDER;
        }

        public void clean() {
            super.clean();
            this.myMessage = null;
        }

        public int hc() {
            return 0;
        }

        public int getEndOffset() {
            return this.myBuilder.myLexStarts[this.myLexemeIndex] + this.myBuilder.myOffset;
        }

        public int getStartOffset() {
            return this.myBuilder.myLexStarts[this.myLexemeIndex] + this.myBuilder.myOffset;
        }

        @NotNull
        public IElementType getTokenType() {
            return TokenType.ERROR_ELEMENT;
        }
    }

    private static class DoneWithErrorMarker extends DoneMarker {
        private String myMessage;

        private DoneWithErrorMarker(@NotNull StartMarker marker, int currentLexeme, String message) {
            super(marker, currentLexeme);
            this.myMessage = message;
        }

        public void clean() {
            super.clean();
            this.myMessage = null;
        }
    }

    private static class DoneMarker extends ProductionMarker {
        private StartMarker myStart;
        private boolean myCollapse;

        public DoneMarker() {
            this.myEdgeTokenBinder = WhitespacesBinders.DEFAULT_RIGHT_BINDER;
        }

        public DoneMarker(StartMarker marker, int currentLexeme) {
            this();
            this.myLexemeIndex = currentLexeme;
            this.myStart = marker;
        }

        public void clean() {
            super.clean();
            this.myStart = null;
            this.myEdgeTokenBinder = WhitespacesBinders.DEFAULT_RIGHT_BINDER;
        }

        public int hc() {
            throw new UnsupportedOperationException("Shall not be called on this kind of markers");
        }

        @NotNull
        public IElementType getTokenType() {
            throw new UnsupportedOperationException("Shall not be called on this kind of markers");
        }

        public int getEndOffset() {
            throw new UnsupportedOperationException("Shall not be called on this kind of markers");
        }

        public int getStartOffset() {
            throw new UnsupportedOperationException("Shall not be called on this kind of markers");
        }
    }

    private static class LazyParseableToken extends Token implements LighterLazyParseableNode {
        private MyTreeStructure myParent;
        private FlyweightCapableTreeStructure<LighterASTNode> myParsed;
        private int myStartIndex;
        private int myEndIndex;

        private LazyParseableToken() {
            super();
        }

        public void clean() {
            super.clean();
            this.myParent = null;
            this.myParsed = null;
        }

        public PsiFile getContainingFile() {
            return this.myBuilder.myFile;
        }

        public CharTable getCharTable() {
            return this.myBuilder.myCharTable;
        }

        public FlyweightCapableTreeStructure<LighterASTNode> parseContents() {
            if (this.myParsed == null) {
                this.myParsed = ((ILightLazyParseableElementType)this.getTokenType()).parseContents(this);
            }

            return this.myParsed;
        }

        public boolean accept(@NotNull LighterLazyParseableNode.Visitor visitor) {
            for(int i = this.myStartIndex; i < this.myEndIndex; ++i) {
                IElementType type = this.myBuilder.myLexTypes[i];
                if (!visitor.visit(type)) {
                    return false;
                }
            }

            return true;
        }
    }

    private static class TokenNode extends Token implements LighterASTTokenNode {
        private TokenNode() {
            super();
        }

        public String toString() {
            return this.getText().toString();
        }
    }

    private abstract static class Token extends Node {
        protected PsiBuilderImpl myBuilder;
        private IElementType myTokenType;
        private int myTokenStart;
        private int myTokenEnd;
        private int myHC;

        private Token() {
            super();
            this.myHC = -1;
        }

        public void clean() {
            this.myBuilder = null;
            this.myHC = -1;
        }

        public int hc() {
            if (this.myHC == -1) {
                int hc = 0;
                //int i;
                if (this.myTokenType instanceof TokenWrapper) {
                    String value = ((TokenWrapper)this.myTokenType).getValue();

                    for(int i = 0; i < value.length(); ++i) {
                        hc += value.charAt(i);
                    }
                } else {
                    int start = this.myTokenStart;
                    final int end = myTokenEnd;
                    CharSequence buf = this.myBuilder.myText;
                    char[] bufArray = this.myBuilder.myTextArray;

                    for(int i = start; i < end; ++i) {
                        hc += bufArray != null ? bufArray[i] : buf.charAt(i);
                    }
                }

                this.myHC = hc;
            }

            return this.myHC;
        }

        public int getEndOffset() {
            return this.myTokenEnd + this.myBuilder.myOffset;
        }

        public int getStartOffset() {
            return this.myTokenStart + this.myBuilder.myOffset;
        }

        @NotNull
        public CharSequence getText() {
            return (CharSequence)(this.myTokenType instanceof TokenWrapper ? ((TokenWrapper)this.myTokenType).getValue() : this.myBuilder.myText.subSequence(this.myTokenStart, this.myTokenEnd));
        }

        @NotNull
        public IElementType getTokenType() {
            return this.myTokenType;
        }
    }

    private static class StartMarker extends ProductionMarker implements Marker {
        private PsiBuilderImpl myBuilder;
        private IElementType myType;
        private DoneMarker myDoneMarker;
        private Throwable myDebugAllocationPosition;
        private ProductionMarker myFirstChild;
        private ProductionMarker myLastChild;
        private int myHC;

        private StartMarker() {
            this.myHC = -1;
            this.myEdgeTokenBinder = WhitespacesBinders.DEFAULT_LEFT_BINDER;
        }

        public void clean() {
            super.clean();
            this.myBuilder = null;
            this.myType = null;
            this.myDoneMarker = null;
            this.myDebugAllocationPosition = null;
            this.myFirstChild = this.myLastChild = null;
            this.myHC = -1;
            this.myEdgeTokenBinder = WhitespacesBinders.DEFAULT_LEFT_BINDER;
        }

        public int hc() {
            if (this.myHC == -1) {
                PsiBuilderImpl builder = this.myBuilder;
                int hc = 0;
                CharSequence buf = builder.myText;
                char[] bufArray = builder.myTextArray;
                ProductionMarker child = this.myFirstChild;

                int lexIdx;
                int lastLeaf;
                for(lexIdx = this.myLexemeIndex; child != null; child = child.myNext) {
                    lastLeaf = child.myLexemeIndex;

                    for(int i = builder.myLexStarts[lexIdx]; i < builder.myLexStarts[lastLeaf]; ++i) {
                        hc += bufArray != null ? bufArray[i] : buf.charAt(i);
                    }

                    lexIdx = lastLeaf;
                    hc += child.hc();
                    if (child instanceof StartMarker) {
                        lexIdx = ((StartMarker)child).myDoneMarker.myLexemeIndex;
                    }
                }

                for(lastLeaf = builder.myLexStarts[lexIdx]; lastLeaf < builder.myLexStarts[this.myDoneMarker.myLexemeIndex]; ++lastLeaf) {
                    hc += bufArray != null ? bufArray[lastLeaf] : buf.charAt(lastLeaf);
                }

                this.myHC = hc;
            }

            return this.myHC;
        }

        public int getStartOffset() {
            return this.myBuilder.myLexStarts[this.myLexemeIndex] + this.myBuilder.myOffset;
        }

        public int getEndOffset() {
            return this.myBuilder.myLexStarts[this.myDoneMarker.myLexemeIndex] + this.myBuilder.myOffset;
        }

        public int getEndIndex() {
            return this.myDoneMarker.myLexemeIndex;
        }

        public void addChild(@NotNull ProductionMarker node) {
            if (this.myFirstChild == null) {
                this.myFirstChild = node;
                this.myLastChild = node;
            } else {
                this.myLastChild.myNext = node;
                this.myLastChild = node;
            }

        }

        @NotNull
        public PsiBuilder.Marker precede() {
            return this.myBuilder.precede(this);
        }

        public void drop() {
            this.myBuilder.drop(this);
        }

        public void rollbackTo() {
            this.myBuilder.rollbackTo(this);
        }

        public void done(@NotNull IElementType type) {
            this.myType = type;
            this.myBuilder.done(this);
        }

        public void collapse(@NotNull IElementType type) {
            this.myType = type;
            this.myBuilder.collapse(this);
        }

        public void doneBefore(@NotNull IElementType type, @NotNull PsiBuilder.Marker before) {
            this.myType = type;
            this.myBuilder.doneBefore(this, before);
        }

        public void doneBefore(@NotNull IElementType type, @NotNull PsiBuilder.Marker before, String errorMessage) {
            StartMarker marker = (StartMarker)before;
            this.myBuilder.myProduction.add(this.myBuilder.myProduction.lastIndexOf(marker), new ErrorItem(this.myBuilder, errorMessage, marker.myLexemeIndex));
            this.doneBefore(type, before);
        }

        public void error(String message) {
            this.myType = TokenType.ERROR_ELEMENT;
            this.myBuilder.error(this, message);
        }

        public void errorBefore(String message, @NotNull PsiBuilder.Marker before) {
            this.myType = TokenType.ERROR_ELEMENT;
            this.myBuilder.errorBefore(this, message, before);
        }

        public IElementType getTokenType() {
            return this.myType;
        }

        public void remapTokenType(@NotNull IElementType type) {
            this.myType = type;
        }

        public void setCustomEdgeTokenBinders(WhitespacesAndCommentsBinder left, WhitespacesAndCommentsBinder right) {
            if (left != null) {
                this.myEdgeTokenBinder = left;
            }

            if (right != null) {
                if (this.myDoneMarker == null) {
                    throw new IllegalArgumentException("Cannot set right-edge processor for unclosed marker");
                }

                this.myDoneMarker.myEdgeTokenBinder = right;
            }

        }

        public String toString() {
            if (this.myBuilder == null) {
                return "<dropped>";
            } else {
                boolean isDone = this.myDoneMarker != null;
                CharSequence originalText = this.myBuilder.getOriginalText();
                int startOffset = this.getStartOffset() - this.myBuilder.myOffset;
                int endOffset = isDone ? this.getEndOffset() - this.myBuilder.myOffset : this.myBuilder.getCurrentOffset();
                CharSequence text = originalText.subSequence(startOffset, endOffset);
                return isDone ? text.toString() : text + "...";
            }
        }
    }

    public abstract static class ProductionMarker extends Node {
        protected int myLexemeIndex;
        protected WhitespacesAndCommentsBinder myEdgeTokenBinder;
        protected ProductionMarker myParent;
        protected ProductionMarker myNext;

        public ProductionMarker() {
            super();
        }

        public void clean() {
            this.myLexemeIndex = 0;
            this.myParent = this.myNext = null;
        }

        public void remapTokenType(@NotNull IElementType type) {
            throw new UnsupportedOperationException("Shall not be called on this kind of markers");
        }

        public int getStartIndex() {
            return this.myLexemeIndex;
        }

        public int getEndIndex() {
            throw new UnsupportedOperationException("Shall not be called on this kind of markers");
        }
    }

    private abstract static class Node implements LighterASTNode {
        private Node() {
        }

        public abstract int hc();
    }
}
