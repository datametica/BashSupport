package psiTree

import org.junit.jupiter.api.Test

class UnmatchedQuotesInBacktickCommandTest : AbstractBashPsiTreeTest() {

  @Test
  fun `test unmatched quotes in backticks test`() {
    val shellCommand = """a=`echo 'a`""".trimMargin()

    val expectedASTNodeString: String = """|ASTWrapperPsiElement(FILE)
                                           |  [PSI] Simple command
                                           |    [PSI] Bash var def
                                           |      PsiElement([Bash] assignment_word)('a')
                                           |      PsiElement([Bash] =)('=')
                                           |      [PSI] bash combined word
                                           |        [PSI] PsiElement(backquote shellcommand)
                                           |          PsiElement([Bash] backquote `)('`')
                                           |          [PSI] Simple command
                                           |            [PSI] BashGenericCommand
                                           |              [PSI] bash combined word
                                           |                PsiElement([Bash] word)('echo')
                                           |            PsiWhiteSpace(' ')
                                           |            [PSI] bash combined word
                                           |              PsiElement([Bash] unevaluated string (STRING2))(''')
                                           |              PsiElement([Bash] unevaluated string (STRING2))('a')
                                           |          PsiElement([Bash] backquote `)('`')
                                           |""".trimMargin()

    assertPsiTree(shellCommand, expectedASTNodeString)
  }

  @Test
  fun `test unmatched quote in backticks immediately before backtick`() {
    val shellCommand = "a=`echo a b'`".trimMargin()

    val expectedASTNodeString: String = """|ASTWrapperPsiElement(FILE)
                                           |  [PSI] Simple command
                                           |    [PSI] Bash var def
                                           |      PsiElement([Bash] assignment_word)('a')
                                           |      PsiElement([Bash] =)('=')
                                           |      [PSI] bash combined word
                                           |        [PSI] PsiElement(backquote shellcommand)
                                           |          PsiElement([Bash] backquote `)('`')
                                           |          [PSI] Simple command
                                           |            [PSI] BashGenericCommand
                                           |              [PSI] bash combined word
                                           |                PsiElement([Bash] word)('echo')
                                           |            PsiWhiteSpace(' ')
                                           |            [PSI] bash combined word
                                           |              PsiElement([Bash] word)('a')
                                           |            PsiWhiteSpace(' ')
                                           |            [PSI] bash combined word
                                           |              PsiElement([Bash] word)('b')
                                           |              PsiElement([Bash] unevaluated string (STRING2))(''')
                                           |          PsiElement([Bash] backquote `)('`')
                                           |""".trimMargin()

    assertPsiTree(shellCommand, expectedASTNodeString)
  }

  @Test
  fun `unmatched quotes in backtick with multiple lines`() {
    val shellCommand = """a=`echo 'foo`
                          |b=`echo 'bar'`""".trimMargin()

    val expectedASTNodeString: String = """|ASTWrapperPsiElement(FILE)
                                           |  [PSI] Simple command
                                           |    [PSI] Bash var def
                                           |      PsiElement([Bash] assignment_word)('a')
                                           |      PsiElement([Bash] =)('=')
                                           |      [PSI] bash combined word
                                           |        [PSI] PsiElement(backquote shellcommand)
                                           |          PsiElement([Bash] backquote `)('`')
                                           |          [PSI] Simple command
                                           |            [PSI] BashGenericCommand
                                           |              [PSI] bash combined word
                                           |                PsiElement([Bash] word)('echo')
                                           |            PsiWhiteSpace(' ')
                                           |            [PSI] bash combined word
                                           |              PsiElement([Bash] unevaluated string (STRING2))(''')
                                           |              PsiElement([Bash] unevaluated string (STRING2))('foo')
                                           |          PsiElement([Bash] backquote `)('`')
                                           |  PsiElement([Bash] linefeed)('\n')
                                           |  [PSI] Simple command
                                           |    [PSI] Bash var def
                                           |      PsiElement([Bash] assignment_word)('b')
                                           |      PsiElement([Bash] =)('=')
                                           |      [PSI] bash combined word
                                           |        [PSI] PsiElement(backquote shellcommand)
                                           |          PsiElement([Bash] backquote `)('`')
                                           |          [PSI] Simple command
                                           |            [PSI] BashGenericCommand
                                           |              [PSI] bash combined word
                                           |                PsiElement([Bash] word)('echo')
                                           |            PsiWhiteSpace(' ')
                                           |            [PSI] bash combined word
                                           |              PsiElement([Bash] unevaluated string (STRING2))(''')
                                           |              PsiElement([Bash] unevaluated string (STRING2))('bar')
                                           |              PsiElement([Bash] unevaluated string (STRING2))(''')
                                           |          PsiElement([Bash] backquote `)('`')
                                           |""".trimMargin()

    assertPsiTree(shellCommand, expectedASTNodeString)
  }
}