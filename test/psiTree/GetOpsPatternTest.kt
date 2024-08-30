package psiTree

import org.junit.jupiter.api.Test
import java.io.File

class GetOpsPatternTest : AbstractBashPsiTreeTest() {

  @Test
  fun `getOps pattern Test1`() {
    val shellCommand =
      """-f@(AE|FGH)_START_AD.*.[0-9][0-9][0-9][0-9][0-9] -d|""".trimMargin()

    val expectedAstNodeString: String = """ASTWrapperPsiElement(FILE)
                                          |  [PSI] Simple command
                                          |    [PSI] BashGenericCommand
                                          |      [PSI] bash combined word
                                          |        PsiElement([Bash] word)('-f@(AE|FGH)_START_AD.*.[0-9][0-9][0-9][0-9][0-9]')
                                          |    PsiWhiteSpace(' ')
                                          |    [PSI] bash combined word
                                          |      PsiElement([Bash] word)('-d|')
                                          |""".trimMargin()

    assertPsiTree(shellCommand, expectedAstNodeString)
  }

  @Test
  fun `getOps pattern Test2`() {
    val shellCommand =
      """getopts "d:" opt;""".trimMargin()

    val expectedAstNodeString: String = """ASTWrapperPsiElement(FILE)
                                          |  [PSI] Simple command
                                          |    [PSI] BashGenericCommand
                                          |      PsiElement([Bash] word)('getopts')
                                          |    PsiWhiteSpace(' ')
                                          |    [PSI] Bash string
                                          |      PsiElement([Bash] string begin)('"')
                                          |      PsiElement([Bash] string content)('d:')
                                          |      PsiElement([Bash] string end)('"')
                                          |    PsiWhiteSpace(' ')
                                          |    [PSI] Bash var def
                                          |      [PSI] bash combined word
                                          |        PsiElement([Bash] word)('opt')
                                          |  PsiElement([Bash] ;)(';')
                                          |""".trimMargin()

    assertPsiTree(shellCommand, expectedAstNodeString)
  }

  @Test
  fun `getOps pattern Test3`() {
    val shellCommand = File("testResources/psiTree/scripts/getOps.txt").readText()

    assertPsiTree(shellCommand, File("testResources/psiTree/expectedPsiTree/getOps.txt").readText())
  }
}