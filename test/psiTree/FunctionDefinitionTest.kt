package psiTree

import org.junit.jupiter.api.Test

class FunctionDefinitionTest: AbstractBashPsiTreeTest() {

    @Test
    fun functionDefinition1() {
        val shellCommand = """function print_message { echo "Hello, this is the print_message function"
    |}""".trimMargin()

        val expectedASTNodeString: String = """ASTWrapperPsiElement(FILE)
                                              |  [PSI] bash function()
                                              |    PsiElement([Bash] function)('function')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] BashFunctionDefName
                                              |      PsiElement([Bash] word)('print_message')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] PsiElement(group element)
                                              |      PsiElement([Bash] {)('{')
                                              |      PsiWhiteSpace(' ')
                                              |      [PSI] Simple command
                                              |        [PSI] BashGenericCommand
                                              |          [PSI] bash combined word
                                              |            PsiElement([Bash] word)('echo')
                                              |        PsiWhiteSpace(' ')
                                              |        [PSI] Bash string
                                              |          PsiElement([Bash] string begin)('"')
                                              |          PsiElement([Bash] string content)('Hello, this is the print_message function')
                                              |          PsiElement([Bash] string end)('"')
                                              |      PsiElement([Bash] linefeed)('\n')
                                              |      PsiElement([Bash] })('}')
                                              |""".trimMargin()

        assertPsiTree(shellCommand, expectedASTNodeString)
    }
}