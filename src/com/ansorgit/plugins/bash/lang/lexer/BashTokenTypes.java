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

package com.ansorgit.plugins.bash.lang.lexer;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

/**
 * Identifies all of the token types (at least, the ones we'll care about) in Arc.
 * Used by the lexer to break a Bash source file down into tokens.
 *
 * @author jansorg
 */
public interface BashTokenTypes {
    IElementType BAD_CHARACTER = TokenType.BAD_CHARACTER;

    // common types
    IElementType WHITESPACE = TokenType.WHITE_SPACE;
    IElementType LINE_CONTINUATION = new BashElementType("line continuation \\");
    TokenSet whitespaceTokens = TokenSet.create(WHITESPACE, LINE_CONTINUATION);

    IElementType ARITH_NUMBER = new BashElementType("number");
    IElementType WORD = new BashElementType("word");
    IElementType ASSIGNMENT_WORD = new BashElementType("assignment_word"); //"a" =2
    IElementType DOLLAR = new BashElementType("$");

    IElementType LEFT_PAREN = new BashElementType("(");
    IElementType RIGHT_PAREN = new BashElementType(")");

    IElementType LEFT_CURLY = new BashElementType("{");
    IElementType RIGHT_CURLY = new BashElementType("}");

    IElementType LEFT_SQUARE = new BashElementType("[ (left square)");
    IElementType RIGHT_SQUARE = new BashElementType("] (right square)");
    TokenSet bracketSet = TokenSet.create(LEFT_SQUARE, RIGHT_SQUARE);

    // comments
    IElementType COMMENT = new BashElementType("Comment");
    IElementType SHEBANG = new BashElementType("Shebang");

    TokenSet commentTokens = TokenSet.create(COMMENT);

    // bash reserved keywords, in alphabetic order
    IElementType CASE_KEYWORD = new BashElementType("case"); //case
    IElementType DO_KEYWORD = new BashElementType("do"); //do
    IElementType DONE_KEYWORD = new BashElementType("done"); //done
    IElementType ELIF_KEYWORD = new BashElementType("elif");//elif
    IElementType ELSE_KEYWORD = new BashElementType("else");//else
    IElementType ESAC_KEYWORD = new BashElementType("esac"); //esac
    IElementType ENDIF_KEYWORD = new BashElementType("endif");//endif
    IElementType FI_KEYWORD = new BashElementType("fi");//fi
    IElementType FOR_KEYWORD = new BashElementType("for");//for
    IElementType FUNCTION_KEYWORD = new BashElementType("function");//function
    IElementType IF_KEYWORD = new BashElementType("if");//if
    IElementType IN_KEYWORD_REMAPPED = new BashElementType("in");//in
    IElementType SELECT_KEYWORD = new BashElementType("select");//select
    IElementType THEN_KEYWORD = new BashElementType("then");//then
    IElementType UNTIL_KEYWORD = new BashElementType("until");//until
    IElementType WHILE_KEYWORD = new BashElementType("while");//while
    IElementType TIME_KEYWORD = new BashElementType("time");//time
    IElementType TRAP_KEYWORD = new BashElementType("trap");//trap
    IElementType LET_KEYWORD = new BashElementType("let");//let
    IElementType BRACKET_KEYWORD = new BashElementType("[[ (left bracket)");//[[
    IElementType _BRACKET_KEYWORD = new BashElementType("]] (right bracket)");//]]

    //case state
    IElementType CASE_END = new BashElementType(";;");//;; for case expressions

    // arithmetic expressions
    IElementType EXPR_ARITH = new BashElementType("((");//))
    IElementType _EXPR_ARITH = new BashElementType("))");//]] after a $((
    IElementType EXPR_ARITH_SQUARE = new BashElementType("[ for arithmetic");
    IElementType _EXPR_ARITH_SQUARE = new BashElementType("] for arithmetic");

    //conditional expressions
    IElementType EXPR_CONDITIONAL = new BashElementType("[ (left conditional)");//"[ "
    IElementType _EXPR_CONDITIONAL = new BashElementType(" ] (right conditional)");//" ]"

    TokenSet keywords = TokenSet.create(CASE_KEYWORD, DO_KEYWORD, DONE_KEYWORD,
            ELIF_KEYWORD, ELSE_KEYWORD, ESAC_KEYWORD, FI_KEYWORD, FOR_KEYWORD, FUNCTION_KEYWORD,
            IF_KEYWORD, IN_KEYWORD_REMAPPED, SELECT_KEYWORD, THEN_KEYWORD, UNTIL_KEYWORD, WHILE_KEYWORD,
            TIME_KEYWORD, BRACKET_KEYWORD, _BRACKET_KEYWORD,
            CASE_END, DOLLAR,
            EXPR_ARITH, _EXPR_ARITH, EXPR_CONDITIONAL, _EXPR_CONDITIONAL);

    TokenSet internalCommands = TokenSet.create(TRAP_KEYWORD, LET_KEYWORD);

    //these are keyword tokens which may be used as identifiers, e.g. in a for loop
    //these tokens will be remapped to word tokens if they occur at a position where a word token would be accepted
    TokenSet identifierKeywords = TokenSet.create(
            CASE_KEYWORD, DO_KEYWORD, DONE_KEYWORD, ELIF_KEYWORD, ELSE_KEYWORD, ESAC_KEYWORD, FI_KEYWORD, FOR_KEYWORD, FUNCTION_KEYWORD,
            IF_KEYWORD, IN_KEYWORD_REMAPPED, SELECT_KEYWORD, THEN_KEYWORD, UNTIL_KEYWORD, WHILE_KEYWORD, TIME_KEYWORD
    );

    // single characters
    IElementType BACKSLASH = new BashElementType("\\");
    IElementType AMP = new BashElementType("&");
    IElementType AT = new BashElementType("@");
    IElementType COLON = new BashElementType(":");
    IElementType COMMA = new BashElementType(",");
    IElementType EQ = new BashElementType("=");
    IElementType ADD_EQ = new BashElementType("+=");
    IElementType SEMI = new BashElementType(";");
    IElementType SHIFT_RIGHT = new BashElementType(">>");//>>
    IElementType LESS_THAN = new BashElementType("<");//>>
    IElementType GREATER_THAN = new BashElementType(">");//>>

    IElementType PIPE = new BashElementType("|");// }
    IElementType PIPE_AMP = new BashElementType("|&"); //bash 4 only, equivalent to 2>&1 |
    IElementType AND_AND = new BashElementType("&&");//!=
    IElementType OR_OR = new BashElementType("||");//!=

    IElementType LINE_FEED = new BashElementType("linefeed");

    TokenSet pipeTokens = TokenSet.create(PIPE, PIPE_AMP);

    //arithmetic operators: plus
    IElementType ARITH_PLUS_PLUS = new BashElementType("++");//++
    IElementType ARITH_PLUS = new BashElementType("+");//+

    //arithmetic operators: minus
    IElementType ARITH_MINUS_MINUS = new BashElementType("--");//++
    IElementType ARITH_MINUS = new BashElementType("-");//+

    TokenSet arithmeticPostOps = TokenSet.create(ARITH_PLUS_PLUS, ARITH_MINUS_MINUS);
    TokenSet arithmeticPreOps = TokenSet.create(ARITH_PLUS_PLUS, ARITH_MINUS_MINUS);
    TokenSet arithmeticAdditionOps = TokenSet.create(ARITH_PLUS, ARITH_MINUS);

    //arithmetic operators: misc
    IElementType ARITH_EXPONENT = new BashElementType("**");//**
    IElementType ARITH_MULT = new BashElementType("*");//*
    IElementType ARITH_DIV = new BashElementType("/");// /
    IElementType ARITH_MOD = new BashElementType("%");//%
    IElementType ARITH_SHIFT_LEFT = new BashElementType("<<");//<<
    IElementType ARITH_SHIFT_RIGHT = new BashElementType(">>");//>>
    IElementType ARITH_NEGATE = new BashElementType("negation !");//||
    IElementType ARITH_BITWISE_NEGATE = new BashElementType("bitwise negation ~");//~

    TokenSet arithmeticShiftOps = TokenSet.create(ARITH_SHIFT_LEFT, ARITH_SHIFT_RIGHT);

    TokenSet arithmeticNegationOps = TokenSet.create(ARITH_NEGATE, ARITH_BITWISE_NEGATE);

    TokenSet arithmeticProduct = TokenSet.create(ARITH_MULT, ARITH_DIV, ARITH_MOD);

    //arithmetic operators: comparision
    IElementType ARITH_LE = new BashElementType("<=");//<=
    IElementType ARITH_GE = new BashElementType(">=");//>=
    IElementType ARITH_GT = new BashElementType("arith >");//>=
    IElementType ARITH_LT = new BashElementType("arith <");//>=

    TokenSet arithmeticCmpOp = TokenSet.create(ARITH_LE, ARITH_GE, ARITH_LT, ARITH_GT);

    IElementType ARITH_EQ = new BashElementType("arith ==");//==
    IElementType ARITH_NE = new BashElementType("!=");//!=
    TokenSet arithmeticEqualityOps = TokenSet.create(ARITH_NE, ARITH_EQ);

    //arithmetic expressiong: logic
    IElementType ARITH_QMARK = new BashElementType("?");//||
    IElementType ARITH_COLON = new BashElementType(":");//||
    IElementType ARITH_BITWISE_XOR = new BashElementType("^");//||
    IElementType ARITH_BITWISE_AND = new BashElementType("&");//||
    //fixme missing: |

    //arithmetic operators: assign
    IElementType ARITH_ASS_MUL = new BashElementType("*= arithmetic");// *=
    IElementType ARITH_ASS_DIV = new BashElementType("/= arithmetic");// /=
    IElementType ARITH_ASS_MOD = new BashElementType("%= arithmetic");// /=
    IElementType ARITH_ASS_PLUS = new BashElementType("+= arithmetic");// /=
    IElementType ARITH_ASS_MINUS = new BashElementType("-= arithmetic");// /=
    IElementType ARITH_ASS_SHIFT_RIGHT = new BashElementType(">>= arithmetic");// /=
    IElementType ARITH_ASS_SHIFT_LEFT = new BashElementType("<<= arithmetic");// /=
    IElementType ARITH_ASS_BIT_AND = new BashElementType("&= arithmetic");// /=
    IElementType ARITH_ASS_BIT_OR = new BashElementType("|= arithmetic");// /=
    IElementType ARITH_ASS_BIT_XOR = new BashElementType("^= arithmetic");// /=
    //fixme missing: = ","

    TokenSet arithmeticAssign = TokenSet.create(ARITH_ASS_MUL, ARITH_ASS_DIV, ARITH_ASS_MOD, ARITH_ASS_PLUS,
            ARITH_ASS_MINUS, ARITH_ASS_SHIFT_LEFT, ARITH_ASS_SHIFT_RIGHT,
            ARITH_ASS_BIT_AND, ARITH_ASS_BIT_OR, ARITH_ASS_BIT_XOR);

    //arithmetic literals
    IElementType ARITH_HEX_NUMBER = new BashElementType("0x hex literal");
    IElementType ARITH_OCTAL_NUMBER = new BashElementType("octal literal");

    IElementType ARITH_BASE_CHAR = new BashElementType("arithmetic base char (#)");

    TokenSet arithLiterals = TokenSet.create(ARITH_NUMBER, ARITH_OCTAL_NUMBER, ARITH_HEX_NUMBER);

    //builtin command
    IElementType COMMAND_TOKEN = new BashElementType("command");//!=
    TokenSet commands = TokenSet.create(COMMAND_TOKEN);

    //variables
    IElementType VARIABLE = new BashElementType("variable");

    //parameter expansion
    IElementType PARAM_EXPANSION_OP_UNKNOWN = new BashElementType("Parameter expansion operator (unknown)");
    IElementType PARAM_EXPANSION_OP_EXCL = new BashElementType("Parameter expansion operator '!'");
    IElementType PARAM_EXPANSION_OP_COLON_EQ = new BashElementType("Parameter expansion operator ':='");
    IElementType PARAM_EXPANSION_OP_COLON_QMARK = new BashElementType("Parameter expansion operator ':?'");
    IElementType PARAM_EXPANSION_OP_EQ = new BashElementType("Parameter expansion operator '='");
    IElementType PARAM_EXPANSION_OP_COLON = new BashElementType("Parameter expansion operator ':'");
    IElementType PARAM_EXPANSION_OP_COLON_MINUS = new BashElementType("Parameter expansion operator ':-'");
    IElementType PARAM_EXPANSION_OP_MINUS = new BashElementType("Parameter expansion operator '-'");
    IElementType PARAM_EXPANSION_OP_COLON_PLUS = new BashElementType("Parameter expansion operator ':+'");
    IElementType PARAM_EXPANSION_OP_PLUS = new BashElementType("Parameter expansion operator '+'");
    IElementType PARAM_EXPANSION_OP_HASH = new BashElementType("Parameter expansion operator '#'");
    IElementType PARAM_EXPANSION_OP_HASH_HASH = new BashElementType("Parameter expansion operator '##'");
    IElementType PARAM_EXPANSION_OP_AT = new BashElementType("Parameter expansion operator '@'");
    IElementType PARAM_EXPANSION_OP_STAR = new BashElementType("Parameter expansion operator '*'");
    IElementType PARAM_EXPANSION_OP_QMARK = new BashElementType("Parameter expansion operator '?'");
    IElementType PARAM_EXPANSION_OP_DOT = new BashElementType("Parameter expansion operator '.'");
    IElementType PARAM_EXPANSION_OP_PERCENT = new BashElementType("Parameter expansion operator '%'");
    IElementType PARAM_EXPANSION_OP_SLASH = new BashElementType("Parameter expansion operator '/'");
    IElementType PARAM_EXPANSION_OP_SLASH_SLASH = new BashElementType("Parameter expansion operator '//'");
    IElementType PARAM_EXPANSION_OP_LOWERCASE_FIRST = new BashElementType("Parameter expansion operator ','");
    IElementType PARAM_EXPANSION_OP_LOWERCASE_ALL = new BashElementType("Parameter expansion operator ',,'");
    IElementType PARAM_EXPANSION_OP_UPPERCASE_FIRST = new BashElementType("Parameter expansion operator '^'");
    IElementType PARAM_EXPANSION_OP_UPPERCASE_ALL = new BashElementType("Parameter expansion operator '^^'");
    IElementType PARAM_EXPANSION_PATTERN = new BashElementType("Parameter expansion regex pattern");
    TokenSet paramExpansionOperators = TokenSet.create(PARAM_EXPANSION_OP_UNKNOWN, PARAM_EXPANSION_OP_EXCL,
            PARAM_EXPANSION_OP_COLON_EQ, PARAM_EXPANSION_OP_COLON_QMARK, PARAM_EXPANSION_OP_EQ, PARAM_EXPANSION_OP_COLON, PARAM_EXPANSION_OP_COLON_MINUS,
            PARAM_EXPANSION_OP_MINUS, PARAM_EXPANSION_OP_PLUS, PARAM_EXPANSION_OP_COLON_PLUS, PARAM_EXPANSION_OP_HASH, PARAM_EXPANSION_OP_HASH_HASH,
            PARAM_EXPANSION_OP_AT, PARAM_EXPANSION_OP_STAR, PARAM_EXPANSION_OP_PERCENT, PARAM_EXPANSION_OP_QMARK, PARAM_EXPANSION_OP_DOT,
            PARAM_EXPANSION_OP_SLASH,PARAM_EXPANSION_OP_SLASH_SLASH,
            PARAM_EXPANSION_OP_LOWERCASE_ALL, PARAM_EXPANSION_OP_LOWERCASE_FIRST,
            PARAM_EXPANSION_OP_UPPERCASE_ALL, PARAM_EXPANSION_OP_UPPERCASE_FIRST,
            PARAM_EXPANSION_PATTERN);
    TokenSet paramExpansionAssignmentOps = TokenSet.create(PARAM_EXPANSION_OP_EQ, PARAM_EXPANSION_OP_COLON_EQ);


    // Special characters
    IElementType STRING_BEGIN = new BashElementType("string begin");
    IElementType STRING_DATA = new BashElementType("string data");
    IElementType STRING_END = new BashElementType("string end");
    //mapped element type
    IElementType STRING_CONTENT = new BashElementType("string content");

    IElementType STRING2 = new BashElementType("unevaluated string (STRING2)");
    IElementType BACKQUOTE = new BashElementType("backquote `");

    IElementType INTEGER_LITERAL = new BashElementType("int literal");

    TokenSet stringLiterals = TokenSet.create(WORD, STRING2, INTEGER_LITERAL, COLON);

    IElementType HEREDOC_MARKER_TAG = new BashElementType("heredoc marker tag");
    IElementType HEREDOC_MARKER_START = new BashElementType("heredoc start marker");
    IElementType HEREDOC_MARKER_END = new BashElementType("heredoc end marker");
    IElementType HEREDOC_MARKER_IGNORING_TABS_END = new BashElementType("heredoc end marker (ignoring tabs)");
    IElementType HEREDOC_LINE = new BashElementType("heredoc line (temporary)");
    IElementType HEREDOC_CONTENT = new BashElementType("here doc content");

    // test Operators
    IElementType COND_OP = new BashElementType("cond_op");//all the test operators, e.g. -z, != ...
    IElementType COND_OP_EQ_EQ = new BashElementType("cond_op ==");
    IElementType COND_OP_REGEX = new BashElementType("cond_op =~");
    IElementType COND_OP_NOT = new BashElementType("cond_op !");
    TokenSet conditionalOperators = TokenSet.create(COND_OP, OR_OR, AND_AND, COND_OP_EQ_EQ, COND_OP_REGEX);

    //redirects
    IElementType REDIRECT_HERE_STRING = new BashElementType("<<<");
    IElementType REDIRECT_LESS_AMP = new BashElementType("<&");
    IElementType REDIRECT_GREATER_AMP = new BashElementType(">&");
    IElementType REDIRECT_LESS_GREATER = new BashElementType("<>");
    IElementType REDIRECT_GREATER_BAR = new BashElementType(">|");
    IElementType FILEDESCRIPTOR = new BashElementType("&[0-9] filedescriptor");

    //Bash 4:
    IElementType REDIRECT_AMP_GREATER_GREATER = new BashElementType("&>>");
    IElementType REDIRECT_GREATER_GREATER_AMP = new BashElementType(">>&");
    IElementType REDIRECT_GREATER_GREATER_AMP_EXCL = new BashElementType(">>&!");
    IElementType REDIRECT_AMP_GREATER = new BashElementType("&>");

    //this must NOT include PIPE_AMP because it's a command separator and not a real redirect token
    TokenSet redirectionSet = TokenSet.create(GREATER_THAN, LESS_THAN, SHIFT_RIGHT,
            REDIRECT_HERE_STRING, REDIRECT_LESS_GREATER,
            REDIRECT_GREATER_BAR, REDIRECT_GREATER_AMP, REDIRECT_AMP_GREATER, REDIRECT_LESS_AMP, REDIRECT_AMP_GREATER_GREATER,
            HEREDOC_MARKER_TAG, REDIRECT_GREATER_GREATER_AMP, REDIRECT_GREATER_GREATER_AMP_EXCL);

    //sets
    TokenSet EQ_SET = TokenSet.create(EQ);
}
