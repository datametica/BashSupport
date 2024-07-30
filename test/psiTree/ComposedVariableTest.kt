package psiTree

import org.junit.jupiter.api.Test

class ComposedVariableTest: AbstractBashPsiTreeTest() {

    @Test
    @Throws(Exception::class)
    fun composedVariableTest() {
        val shellContent = """PROG=${'$'}{0##*/}
                            |PROG=${'$'}{PROG%.*}
                            |""".trimMargin()

        val expectedPsiTree = """ASTWrapperPsiElement(FILE)
|  [PSI] Simple command
|    [PSI] Bash var def
|      PsiElement([Bash] assignment_word)('PROG')
|      PsiElement([Bash] =)('=')
|      [PSI] bash combined word
|        [PSI] BashComposedVar
|          PsiElement([Bash] ${'$'})('${'$'}')
|          PsiElement([Bash] {)('{')
|          [PSI] Bash-var
|            PsiElement([Bash] word)('0')
|          PsiElement([Bash] Parameter expansion operator '##')('##')
|          PsiElement([Bash] word)('*/')
|          PsiElement([Bash] })('}')
|  PsiElement([Bash] linefeed)('\n')
|  [PSI] Simple command
|    [PSI] Bash var def
|      PsiElement([Bash] assignment_word)('PROG')
|      PsiElement([Bash] =)('=')
|      [PSI] bash combined word
|        [PSI] BashComposedVar
|          PsiElement([Bash] ${'$'})('${'$'}')
|          PsiElement([Bash] {)('{')
|          [PSI] Bash-var
|            PsiElement([Bash] word)('PROG')
|          PsiElement([Bash] Parameter expansion operator '%')('%')
|          PsiElement([Bash] word)('.*')
|          PsiElement([Bash] })('}')
|  PsiElement([Bash] linefeed)('\n')
|""".trimMargin()

        assertPsiTree(shellContent, expectedPsiTree)
    }
}