package psiTree

import org.junit.jupiter.api.Test

class BashPsiTreeTest: AbstractBashPsiTreeTest() {

    @Test
    @Throws(Exception::class)
    fun testShiftLeft() {
        val shellCommand = """a=128
                        |b="this is assignment test!"
    """.trimMargin()

        val expectedASTNodeString: String = """ASTWrapperPsiElement(FILE)
                                              |  [PSI] Simple command
                                              |    [PSI] Bash var def
                                              |      PsiElement([Bash] assignment_word)('a')
                                              |      PsiElement([Bash] =)('=')
                                              |      [PSI] bash combined word
                                              |        PsiElement([Bash] int literal)('128')
                                              |  PsiElement([Bash] linefeed)('\n')
                                              |  [PSI] Simple command
                                              |    [PSI] Bash var def
                                              |      PsiElement([Bash] assignment_word)('b')
                                              |      PsiElement([Bash] =)('=')
                                              |      [PSI] Bash string
                                              |        PsiElement([Bash] string begin)('"')
                                              |        PsiElement([Bash] string content)('this is assignment test!')
                                              |        PsiElement([Bash] string end)('"')
                                              |""".trimMargin()

        assertPsiTree(shellCommand, expectedASTNodeString)
    }

    @Test
    fun echoCommandTest() {
        val bashCommand = """echo "echo command test!"
    """.trimMargin()

        val expectedASTNodeString: String = """ASTWrapperPsiElement(FILE)
                                              |  [PSI] Simple command
                                              |    [PSI] BashGenericCommand
                                              |      [PSI] bash combined word
                                              |        PsiElement([Bash] word)('echo')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] Bash string
                                              |      PsiElement([Bash] string begin)('"')
                                              |      PsiElement([Bash] string content)('echo command test!')
                                              |      PsiElement([Bash] string end)('"')
                                              |""".trimMargin()

        assertPsiTree(bashCommand, expectedASTNodeString)
    }

    @Test
    fun forCommandTest() {
        val bashCommand = """for i in 1 2 3
                        |do
                        |   echo "iterating..."
                        |done
    """.trimMargin()

        val expectedASTNodeString: String = """ASTWrapperPsiElement(FILE)
                                              |  [PSI] PsiElement(for shellcommand)
                                              |    PsiElement([Bash] for)('for')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] Bash var def
                                              |      PsiElement([Bash] word)('i')
                                              |    PsiWhiteSpace(' ')
                                              |    PsiElement([Bash] in)('in')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] bash combined word
                                              |      PsiElement([Bash] int literal)('1')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] bash combined word
                                              |      PsiElement([Bash] int literal)('2')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] bash combined word
                                              |      PsiElement([Bash] int literal)('3')
                                              |    PsiElement([Bash] linefeed)('\n')
                                              |    PsiElement([Bash] do)('do')
                                              |    [PSI] PsiElement(logical block)
                                              |      PsiElement([Bash] linefeed)('\n')
                                              |      PsiWhiteSpace(' ')
                                              |      PsiWhiteSpace(' ')
                                              |      PsiWhiteSpace(' ')
                                              |      [PSI] Simple command
                                              |        [PSI] BashGenericCommand
                                              |          [PSI] bash combined word
                                              |            PsiElement([Bash] word)('echo')
                                              |        PsiWhiteSpace(' ')
                                              |        [PSI] Bash string
                                              |          PsiElement([Bash] string begin)('"')
                                              |          PsiElement([Bash] string content)('iterating...')
                                              |          PsiElement([Bash] string end)('"')
                                              |    PsiElement([Bash] linefeed)('\n')
                                              |    PsiElement([Bash] done)('done')
                                              |""".trimMargin()

        assertPsiTree(bashCommand, expectedASTNodeString)
    }

}