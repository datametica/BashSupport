package psiTree

import org.junit.jupiter.api.Test

class EvalCommandTest : AbstractBashPsiTreeTest() {

  @Test
  fun `eval command Test containing array within array index position`() {
    val shellCommand =
      """array=("x" "y")
        |eval typeset array['$'{#array[*]}]="value"""".trimMargin()

    val expectedAstNodeString: String = """ASTWrapperPsiElement(FILE)
                                          |  [PSI] Simple command
                                          |    [PSI] Bash var def
                                          |      PsiElement([Bash] assignment_word)('array')
                                          |      PsiElement([Bash] =)('=')
                                          |      [PSI] assignment list
                                          |        PsiElement([Bash] ()('(')
                                          |        [PSI] Bash string
                                          |          PsiElement([Bash] string begin)('"')
                                          |          PsiElement([Bash] string content)('x')
                                          |          PsiElement([Bash] string end)('"')
                                          |        PsiWhiteSpace(' ')
                                          |        [PSI] Bash string
                                          |          PsiElement([Bash] string begin)('"')
                                          |          PsiElement([Bash] string content)('y')
                                          |          PsiElement([Bash] string end)('"')
                                          |        PsiElement([Bash] ))(')')
                                          |  PsiElement([Bash] linefeed)('\n')
                                          |  PsiElement([Bash] word)('eval')
                                          |  PsiWhiteSpace(' ')
                                          |  PsiElement(eval block)('typeset')
                                          |  PsiWhiteSpace(' ')
                                          |  PsiElement(eval block)('array['${'$'}'{#array[*]}]=')
                                          |  PsiElement(eval block)('"value"')
                                          |""".trimMargin()

    assertPsiTree(shellCommand, expectedAstNodeString)
  }

  @Test
  fun `eval command followed by a comment`() {
    val shellCommand =
      """array=("x" "y")
        |eval typeset array['$'{#array[*]}]="value"
        | #==this is a comment==""".trimMargin()

    val expectedAstNodeString: String = """ASTWrapperPsiElement(FILE)
                                          |  [PSI] Simple command
                                          |    [PSI] Bash var def
                                          |      PsiElement([Bash] assignment_word)('array')
                                          |      PsiElement([Bash] =)('=')
                                          |      [PSI] assignment list
                                          |        PsiElement([Bash] ()('(')
                                          |        [PSI] Bash string
                                          |          PsiElement([Bash] string begin)('"')
                                          |          PsiElement([Bash] string content)('x')
                                          |          PsiElement([Bash] string end)('"')
                                          |        PsiWhiteSpace(' ')
                                          |        [PSI] Bash string
                                          |          PsiElement([Bash] string begin)('"')
                                          |          PsiElement([Bash] string content)('y')
                                          |          PsiElement([Bash] string end)('"')
                                          |        PsiElement([Bash] ))(')')
                                          |  PsiElement([Bash] linefeed)('\n')
                                          |  PsiElement([Bash] word)('eval')
                                          |  PsiWhiteSpace(' ')
                                          |  PsiElement(eval block)('typeset')
                                          |  PsiWhiteSpace(' ')
                                          |  PsiElement(eval block)('array['${'$'}'{#array[*]}]=')
                                          |  PsiElement(eval block)('"value"')
                                          |  PsiElement([Bash] linefeed)('\n')
                                          |  PsiWhiteSpace(' ')
                                          |  PsiComment([Bash] Comment)('#==this is a comment==')
                                          |""".trimMargin()

      assertPsiTree(shellCommand, expectedAstNodeString)
  }

  @Test
  fun `eval command Test containing variable within array index position`() {
    val shellCommand =
      """b=1
        |array=("x" "y")
        |eval typeset array['$'{b}]="value"""".trimMargin()

    val expectedAstNodeString: String = """ASTWrapperPsiElement(FILE)
                                          |  [PSI] Simple command
                                          |    [PSI] Bash var def
                                          |      PsiElement([Bash] assignment_word)('b')
                                          |      PsiElement([Bash] =)('=')
                                          |      [PSI] bash combined word
                                          |        PsiElement([Bash] int literal)('1')
                                          |  PsiElement([Bash] linefeed)('\n')
                                          |  [PSI] Simple command
                                          |    [PSI] Bash var def
                                          |      PsiElement([Bash] assignment_word)('array')
                                          |      PsiElement([Bash] =)('=')
                                          |      [PSI] assignment list
                                          |        PsiElement([Bash] ()('(')
                                          |        [PSI] Bash string
                                          |          PsiElement([Bash] string begin)('"')
                                          |          PsiElement([Bash] string content)('x')
                                          |          PsiElement([Bash] string end)('"')
                                          |        PsiWhiteSpace(' ')
                                          |        [PSI] Bash string
                                          |          PsiElement([Bash] string begin)('"')
                                          |          PsiElement([Bash] string content)('y')
                                          |          PsiElement([Bash] string end)('"')
                                          |        PsiElement([Bash] ))(')')
                                          |  PsiElement([Bash] linefeed)('\n')
                                          |  PsiElement([Bash] word)('eval')
                                          |  PsiWhiteSpace(' ')
                                          |  PsiElement(eval block)('typeset')
                                          |  PsiWhiteSpace(' ')
                                          |  PsiElement(eval block)('array['${'$'}'{b}]=')
                                          |  PsiElement(eval block)('"value"')
                                          |""".trimMargin()

      assertPsiTree(shellCommand, expectedAstNodeString)
  }
}