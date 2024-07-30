package psiTree

import org.junit.jupiter.api.Test

class WildcardOperatorPatternTest : AbstractBashPsiTreeTest() {

  @Test
  fun `Wildcard Operator Pattern Test`() {
    val shellCommand = "[[ \'ENOTIFY\' == @(*NOTIFY*) ]]".trimMargin()
    val expectedASTNodeString: String = """ASTWrapperPsiElement(FILE)
                                              |  [PSI] PsiElement(extended conditional shellcommand)
                                              |    PsiElement([Bash] [[ (left bracket))('[[ ')
                                              |    [PSI] bash combined word
                                              |      PsiElement([Bash] unevaluated string (STRING2))(''ENOTIFY'')
                                              |    PsiWhiteSpace(' ')
                                              |    PsiElement([Bash] cond_op ==)('==')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] bash combined word
                                              |      [PSI] BashComposedVar
                                              |        PsiElement([Bash] @)('@')
                                              |        [PSI] PsiElement(subshell shellcommand)
                                              |          PsiElement([Bash] ()('(')
                                              |          [PSI] Simple command
                                              |            [PSI] BashGenericCommand
                                              |              [PSI] bash combined word
                                              |                PsiElement([Bash] word)('*NOTIFY*')
                                              |          PsiElement([Bash] ))(')')
                                              |    PsiElement([Bash] ]] (right bracket))(' ]]')
                                              |""".trimMargin()
    assertPsiTree(shellCommand, expectedASTNodeString)
  }

  @Test
  fun `Wildcard Operator in comment Test`() {
    val shellCommand = "# \"@(#)sort.csh\"".trimMargin()
    val expectedASTNodeString: String = """ASTWrapperPsiElement(FILE)
                                              |  PsiComment([Bash] Comment)('# "@(#)sort.csh"')
                                              |""".trimMargin()
    assertPsiTree(shellCommand, expectedASTNodeString)
  }
}