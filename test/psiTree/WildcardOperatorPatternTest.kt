package psiTree

import org.junit.jupiter.api.Test

class WildcardOperatorPatternTest : AbstractBashPsiTreeTest() {

  @Test
  fun `Wildcard Operator Pattern Test`() {
    val shellCommand = "[[ \'ABCD\' == @(*BCD*) ]]".trimMargin()
    val expectedASTNodeString: String = """ASTWrapperPsiElement(FILE)
                                              |  [PSI] PsiElement(extended conditional shellcommand)
                                              |    PsiElement([Bash] [[ (left bracket))('[[ ')
                                              |    [PSI] bash combined word
                                              |      PsiElement([Bash] unevaluated string (STRING2))(''ABCD'')
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
                                              |                PsiElement([Bash] word)('*BCD*')
                                              |          PsiElement([Bash] ))(')')
                                              |    PsiElement([Bash] ]] (right bracket))(' ]]')
                                              |""".trimMargin()
    assertPsiTree(shellCommand, expectedASTNodeString)
  }

  @Test
  fun `Wildcard Operator in comment Test`() {
    val shellCommand = "# \"@(#)demo.csh\"".trimMargin()
    val expectedASTNodeString: String = """ASTWrapperPsiElement(FILE)
                                              |  PsiComment([Bash] Comment)('# "@(#)demo.csh"')
                                              |""".trimMargin()
    assertPsiTree(shellCommand, expectedASTNodeString)
  }
}