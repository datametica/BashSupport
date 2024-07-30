/*
 * Copyright (c) Joachim Ansorg, mail@ansorg-it.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ansorgit.plugins.bash.lang.parser;

import com.ansorgit.plugins.bash.lang.BashVersion;
import com.ansorgit.plugins.bash.lang.lexer.BashLexer;
import com.ansorgit.plugins.bash.lang.lexer.BashTokenTypes;
import com.ansorgit.plugins.bash.lang.psi.BashPsiCreator;
import com.ansorgit.plugins.bash.lang.psi.impl.BashFileImpl;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

/**
 * Defines the implementation of the Bash parser. This is  the starting point for the parse.
 * This class is referenced in the plugin.xml file.
 *
 * @author jansorg
 */
public class BashParserDefinition implements ParserDefinition, BashElementTypes {
    // These tokens are used e.g. during the "find references in strings" search
    private static final TokenSet stringLiterals = TokenSet.create(BashTokenTypes.STRING2, BashTokenTypes.INTEGER_LITERAL, BashTokenTypes.COLON, BashElementTypes.STRING_ELEMENT);

    @NotNull
    public Lexer createLexer() {
        return createBashLexer();
    }

    public static Lexer createBashLexer() {
        return new BashLexer(findLanguageLevel());
    }

    public PsiParser createParser() {
        return new BashParser(findLanguageLevel());
    }

    public static PsiParser createNewParser() {
        return new BashParser(findLanguageLevel());
    }

    private static BashVersion findLanguageLevel() {
        boolean supportBash4 = true;
        return supportBash4 ? BashVersion.Bash_v4 : BashVersion.Bash_v3;
    }

    public IFileElementType getFileNodeType() {
        return FILE;
    }

    @NotNull
    public TokenSet getWhitespaceTokens() {
        return BashTokenTypes.whitespaceTokens;
    }

    @NotNull
    public TokenSet getCommentTokens() {
        return BashTokenTypes.commentTokens;
    }

    @NotNull
    public TokenSet getStringLiteralElements() {
        return stringLiterals;
    }

    private static final TokenSet heredocTokens = TokenSet.create(BashTokenTypes.HEREDOC_CONTENT,
            BashTokenTypes.HEREDOC_MARKER_START, BashTokenTypes.HEREDOC_MARKER_END, BashTokenTypes.HEREDOC_MARKER_IGNORING_TABS_END);

    public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode leftAst, ASTNode rightAst) {
        final IElementType left = leftAst.getElementType();
        final IElementType right = rightAst.getElementType();

        if (left == BashTokenTypes.LINE_FEED
                || right == BashTokenTypes.LINE_FEED
                || left == BashTokenTypes.ASSIGNMENT_WORD) {
            return SpaceRequirements.MUST_NOT;
        }

        //heredoc
        if (heredocTokens.contains(left) || heredocTokens.contains(right)) {
            return SpaceRequirements.MUST_NOT;
        }

        if (left == BashTokenTypes.LEFT_PAREN
                || right == BashTokenTypes.RIGHT_PAREN
                || left == BashTokenTypes.RIGHT_PAREN
                || right == BashTokenTypes.LEFT_PAREN

                || left == BashTokenTypes.LEFT_CURLY
                || right == BashTokenTypes.LEFT_CURLY
                || left == BashTokenTypes.RIGHT_CURLY
                || right == BashTokenTypes.RIGHT_CURLY

                || (left == BashTokenTypes.WORD && right == BashTokenTypes.PARAM_EXPANSION_OP_UNKNOWN)

                || left == BashTokenTypes.LEFT_SQUARE
                || right == BashTokenTypes.RIGHT_SQUARE
                || left == BashTokenTypes.RIGHT_SQUARE
                || right == BashTokenTypes.LEFT_SQUARE

                || left == BashTokenTypes.VARIABLE
                || right == BashTokenTypes.VARIABLE) {

            return SpaceRequirements.MAY;
        }

        return SpaceRequirements.MUST;
    }

    @NotNull
    public PsiElement createElement(ASTNode node) {
        return BashPsiCreator.createElement(node);
    }

    public PsiFile createFile(FileViewProvider viewProvider) {
        return new BashFileImpl(viewProvider);
    }
}
