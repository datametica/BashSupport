package psiTree

import org.junit.jupiter.api.Test

class TypesetStatementTest: AbstractBashPsiTreeTest() {

    @Test
    fun typesetCommand1() {
        val shellCommand = """typeset -l c=ABC""".trimMargin()

        val expectedASTNodeString: String = """ASTWrapperPsiElement(FILE)
                                              |  [PSI] Simple command
                                              |    [PSI] BashGenericCommand
                                              |      PsiElement([Bash] word)('typeset')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] bash combined word
                                              |      PsiElement([Bash] word)('-l')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] Bash var def
                                              |      PsiElement([Bash] assignment_word)('c')
                                              |      PsiElement([Bash] =)('=')
                                              |      [PSI] bash combined word
                                              |        PsiElement([Bash] word)('ABC')
                                              |""".trimMargin()

        assertPsiTree(shellCommand, expectedASTNodeString)
    }

    @Test
    fun typesetCommand2() {
        val shellCommand = """typeset -u c=abc d=xyz""".trimMargin()

        val expectedASTNodeString: String = """ASTWrapperPsiElement(FILE)
                                              |  [PSI] Simple command
                                              |    [PSI] BashGenericCommand
                                              |      PsiElement([Bash] word)('typeset')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] bash combined word
                                              |      PsiElement([Bash] word)('-u')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] Bash var def
                                              |      PsiElement([Bash] assignment_word)('c')
                                              |      PsiElement([Bash] =)('=')
                                              |      [PSI] bash combined word
                                              |        PsiElement([Bash] word)('abc')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] Bash var def
                                              |      PsiElement([Bash] assignment_word)('d')
                                              |      PsiElement([Bash] =)('=')
                                              |      [PSI] bash combined word
                                              |        PsiElement([Bash] word)('xyz')
                                              |""".trimMargin()

        assertPsiTree(shellCommand, expectedASTNodeString)
    }

    @Test
    fun typesetCommand3() {
        val shellCommand = """testFun() { echo 'abc' };
                             |typeset -pf testFun""".trimMargin()

        val expectedASTNodeString: String = """ASTWrapperPsiElement(FILE)
                                              |  [PSI] bash function()
                                              |    [PSI] BashFunctionDefName
                                              |      PsiElement([Bash] word)('testFun')
                                              |    PsiElement([Bash] ()('(')
                                              |    PsiElement([Bash] ))(')')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] PsiElement(group element)
                                              |      PsiElement([Bash] {)('{')
                                              |      PsiWhiteSpace(' ')
                                              |      [PSI] Simple command
                                              |        [PSI] BashGenericCommand
                                              |          [PSI] bash combined word
                                              |            PsiElement([Bash] word)('echo')
                                              |        PsiWhiteSpace(' ')
                                              |        [PSI] bash combined word
                                              |          PsiElement([Bash] unevaluated string (STRING2))(''abc'')
                                              |      PsiWhiteSpace(' ')
                                              |      PsiElement([Bash] })('}')
                                              |    PsiElement([Bash] ;)(';')
                                              |  PsiElement([Bash] linefeed)('\n')
                                              |  [PSI] Simple command
                                              |    [PSI] BashGenericCommand
                                              |      PsiElement([Bash] word)('typeset')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] bash combined word
                                              |      PsiElement([Bash] word)('-pf')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] Bash var def
                                              |      [PSI] bash combined word
                                              |        PsiElement([Bash] word)('testFun')
                                              |""".trimMargin()

        assertPsiTree(shellCommand, expectedASTNodeString)
    }
}