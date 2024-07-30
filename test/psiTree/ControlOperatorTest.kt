package psiTree

import org.junit.jupiter.api.Test
import java.io.File

class ControlOperatorTest: AbstractBashPsiTreeTest() {

    @Test
    fun controlOperator1() {
        val shellCommand = "echo first && echo second"

        val expectedASTNodeString: String = """ASTWrapperPsiElement(FILE)
                                              |  [PSI] Simple command
                                              |    [PSI] BashGenericCommand
                                              |      [PSI] bash combined word
                                              |        PsiElement([Bash] word)('echo')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] bash combined word
                                              |      PsiElement([Bash] word)('first')
                                              |  PsiWhiteSpace(' ')
                                              |  PsiElement([Bash] &&)('&&')
                                              |  PsiWhiteSpace(' ')
                                              |  [PSI] Simple command
                                              |    [PSI] BashGenericCommand
                                              |      [PSI] bash combined word
                                              |        PsiElement([Bash] word)('echo')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] bash combined word
                                              |      PsiElement([Bash] word)('second')
                                              |""".trimMargin()

        assertPsiTree(shellCommand, expectedASTNodeString)
    }

    @Test
    fun controlOperator2() {
        val shellCommand = "echo first || echo second"

        val expectedASTNodeString: String = """ASTWrapperPsiElement(FILE)
                                              |  [PSI] Simple command
                                              |    [PSI] BashGenericCommand
                                              |      [PSI] bash combined word
                                              |        PsiElement([Bash] word)('echo')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] bash combined word
                                              |      PsiElement([Bash] word)('first')
                                              |  PsiWhiteSpace(' ')
                                              |  PsiElement([Bash] ||)('||')
                                              |  PsiWhiteSpace(' ')
                                              |  [PSI] Simple command
                                              |    [PSI] BashGenericCommand
                                              |      [PSI] bash combined word
                                              |        PsiElement([Bash] word)('echo')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] bash combined word
                                              |      PsiElement([Bash] word)('second')
                                              |""".trimMargin()

        assertPsiTree(shellCommand, expectedASTNodeString)
    }

    @Test
    fun controlOperator3() {
        val shellCommand = "assignment_variable=10 ; echo \"This is semi colon element\"\n" +
                " echo \$assignment_variable"

        assertPsiTree(shellCommand, File("testResources/psiTree/expectedPsiTree/semicolonSeperated.txt").readText())
    }
}