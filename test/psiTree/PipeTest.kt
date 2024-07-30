package psiTree

import org.junit.jupiter.api.Test

class PipeTest: AbstractBashPsiTreeTest() {

    @Test
    fun pipe1() {
        val shellCommand = "cat src/test/resources/txt/alphabetsInput.txt | sort -u | tail -1"

        val expectedASTNodeString: String = """ASTWrapperPsiElement(FILE)
                                              |  [PSI] pipeline command
                                              |    [PSI] Simple command
                                              |      [PSI] BashGenericCommand
                                              |        [PSI] bash combined word
                                              |          PsiElement([Bash] word)('cat')
                                              |      PsiWhiteSpace(' ')
                                              |      [PSI] bash combined word
                                              |        PsiElement([Bash] word)('src/test/resources/txt/alphabetsInput.txt')
                                              |    PsiWhiteSpace(' ')
                                              |    PsiElement([Bash] |)('|')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] Simple command
                                              |      [PSI] BashGenericCommand
                                              |        [PSI] bash combined word
                                              |          PsiElement([Bash] word)('sort')
                                              |      PsiWhiteSpace(' ')
                                              |      [PSI] bash combined word
                                              |        PsiElement([Bash] word)('-u')
                                              |    PsiWhiteSpace(' ')
                                              |    PsiElement([Bash] |)('|')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] Simple command
                                              |      [PSI] BashGenericCommand
                                              |        [PSI] bash combined word
                                              |          PsiElement([Bash] word)('tail')
                                              |      PsiWhiteSpace(' ')
                                              |      [PSI] bash combined word
                                              |        PsiElement([Bash] word)('-1')
                                              |""".trimMargin()

        assertPsiTree(shellCommand, expectedASTNodeString)
    }

    @Test
    fun pipe2() {
        val shellCommand = "echo aabbccdd | sed 's/b/k/g'"

        val expectedASTNodeString: String = """ASTWrapperPsiElement(FILE)
                                              |  [PSI] pipeline command
                                              |    [PSI] Simple command
                                              |      [PSI] BashGenericCommand
                                              |        [PSI] bash combined word
                                              |          PsiElement([Bash] word)('echo')
                                              |      PsiWhiteSpace(' ')
                                              |      [PSI] bash combined word
                                              |        PsiElement([Bash] word)('aabbccdd')
                                              |    PsiWhiteSpace(' ')
                                              |    PsiElement([Bash] |)('|')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] Simple command
                                              |      [PSI] BashGenericCommand
                                              |        [PSI] bash combined word
                                              |          PsiElement([Bash] word)('sed')
                                              |      PsiWhiteSpace(' ')
                                              |      [PSI] bash combined word
                                              |        PsiElement([Bash] unevaluated string (STRING2))(''s/b/k/g'')
                                              |""".trimMargin()

        assertPsiTree(shellCommand, expectedASTNodeString)
    }
}