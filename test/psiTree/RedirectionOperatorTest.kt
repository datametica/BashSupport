package psiTree

import org.junit.jupiter.api.Test


class RedirectionOperatorTest : AbstractBashPsiTreeTest() {

  @Test
  fun redirectGreaterGreaterAmpTest() {
    val shellCommand = """./demoFile.csh >>& log.txt
                         |echo abc >>& log.txt
                         |grep -i error log.txt >>& output.txt""".trimMargin()

    val expectedASTNodeString: String = """ASTWrapperPsiElement(FILE)
                                              |  [PSI] Simple command
                                              |    [PSI] BashGenericCommand
                                              |      [PSI] bash combined word
                                              |        PsiElement([Bash] word)('./demoFile.csh')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] BashRedirectList
                                              |      [PSI] BashRedirectExpr
                                              |        PsiElement([Bash] >>&)('>>&')
                                              |        PsiWhiteSpace(' ')
                                              |        [PSI] bash combined word
                                              |          PsiElement([Bash] word)('log.txt')
                                              |  PsiElement([Bash] linefeed)('\n')
                                              |  [PSI] Simple command
                                              |    [PSI] BashGenericCommand
                                              |      [PSI] bash combined word
                                              |        PsiElement([Bash] word)('echo')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] bash combined word
                                              |      PsiElement([Bash] word)('abc')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] BashRedirectList
                                              |      [PSI] BashRedirectExpr
                                              |        PsiElement([Bash] >>&)('>>&')
                                              |        PsiWhiteSpace(' ')
                                              |        [PSI] bash combined word
                                              |          PsiElement([Bash] word)('log.txt')
                                              |  PsiElement([Bash] linefeed)('\n')
                                              |  [PSI] Simple command
                                              |    [PSI] BashGenericCommand
                                              |      [PSI] bash combined word
                                              |        PsiElement([Bash] word)('grep')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] bash combined word
                                              |      PsiElement([Bash] word)('-i')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] bash combined word
                                              |      PsiElement([Bash] word)('error')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] bash combined word
                                              |      PsiElement([Bash] word)('log.txt')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] BashRedirectList
                                              |      [PSI] BashRedirectExpr
                                              |        PsiElement([Bash] >>&)('>>&')
                                              |        PsiWhiteSpace(' ')
                                              |        [PSI] bash combined word
                                              |          PsiElement([Bash] word)('output.txt')
                                              |""".trimMargin()

    assertPsiTree(shellCommand, expectedASTNodeString)
  }

  @Test
  fun redirectGreaterGreaterAmpExclTest() {
    val shellCommand = """./demoFile.csh >>&! log.txt
                         |echo abc >>&! log.txt
                         |grep -i error log.txt >>&! output.txt""".trimMargin()

    val expectedASTNodeString: String = """ASTWrapperPsiElement(FILE)
                                              |  [PSI] Simple command
                                              |    [PSI] BashGenericCommand
                                              |      [PSI] bash combined word
                                              |        PsiElement([Bash] word)('./demoFile.csh')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] BashRedirectList
                                              |      [PSI] BashRedirectExpr
                                              |        PsiElement([Bash] >>&!)('>>&!')
                                              |        PsiWhiteSpace(' ')
                                              |        [PSI] bash combined word
                                              |          PsiElement([Bash] word)('log.txt')
                                              |  PsiElement([Bash] linefeed)('\n')
                                              |  [PSI] Simple command
                                              |    [PSI] BashGenericCommand
                                              |      [PSI] bash combined word
                                              |        PsiElement([Bash] word)('echo')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] bash combined word
                                              |      PsiElement([Bash] word)('abc')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] BashRedirectList
                                              |      [PSI] BashRedirectExpr
                                              |        PsiElement([Bash] >>&!)('>>&!')
                                              |        PsiWhiteSpace(' ')
                                              |        [PSI] bash combined word
                                              |          PsiElement([Bash] word)('log.txt')
                                              |  PsiElement([Bash] linefeed)('\n')
                                              |  [PSI] Simple command
                                              |    [PSI] BashGenericCommand
                                              |      [PSI] bash combined word
                                              |        PsiElement([Bash] word)('grep')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] bash combined word
                                              |      PsiElement([Bash] word)('-i')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] bash combined word
                                              |      PsiElement([Bash] word)('error')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] bash combined word
                                              |      PsiElement([Bash] word)('log.txt')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] BashRedirectList
                                              |      [PSI] BashRedirectExpr
                                              |        PsiElement([Bash] >>&!)('>>&!')
                                              |        PsiWhiteSpace(' ')
                                              |        [PSI] bash combined word
                                              |          PsiElement([Bash] word)('output.txt')
                                              |""".trimMargin()

    assertPsiTree(shellCommand, expectedASTNodeString)
  }
}