package psiTree

import org.junit.jupiter.api.Test

class ReadCommandTest: AbstractBashPsiTreeTest() {

    @Test
    fun read1() {
        val shellCommand = "read -i \"test_\" var1 var2 <<< \"value1 value2\n\""

        val expectedASTNodeString: String = """ASTWrapperPsiElement(FILE)
                                              |  [PSI] Simple command
                                              |    [PSI] BashGenericCommand
                                              |      PsiElement([Bash] word)('read')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] bash combined word
                                              |      PsiElement([Bash] word)('-i')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] Bash string
                                              |      PsiElement([Bash] string begin)('"')
                                              |      PsiElement([Bash] string content)('test_')
                                              |      PsiElement([Bash] string end)('"')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] Bash var def
                                              |      [PSI] bash combined word
                                              |        PsiElement([Bash] word)('var1')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] Bash var def
                                              |      [PSI] bash combined word
                                              |        PsiElement([Bash] word)('var2')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] BashRedirectExpr
                                              |      PsiElement([Bash] <<<)('<<<')
                                              |      PsiWhiteSpace(' ')
                                              |      [PSI] Bash string
                                              |        PsiElement([Bash] string begin)('"')
                                              |        PsiElement([Bash] string content)('value1 value2\n')
                                              |        PsiElement([Bash] string end)('"')
                                              |""".trimMargin()

        assertPsiTree(shellCommand, expectedASTNodeString)
    }
}