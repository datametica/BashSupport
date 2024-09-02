package psiTree

import org.junit.jupiter.api.Test

class UnsetStatementTest: AbstractBashPsiTreeTest() {

    @Test
    fun unset1() {
        val shellCommand = "unset -f print_message"

        val expectedASTNodeString: String = """ASTWrapperPsiElement(FILE)
                                              |  [PSI] Simple command
                                              |    [PSI] BashGenericCommand
                                              |      [PSI] bash combined word
                                              |        PsiElement([Bash] word)('unset')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] bash combined word
                                              |      PsiElement([Bash] word)('-f')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] bash combined word
                                              |      PsiElement([Bash] word)('print_message')
                                              |""".trimMargin()

        assertPsiTree(shellCommand, expectedASTNodeString)
    }

}