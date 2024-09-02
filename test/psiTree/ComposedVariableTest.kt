package psiTree

import org.junit.jupiter.api.Test

class ComposedVariableTest: AbstractBashPsiTreeTest() {

    @Test
    @Throws(Exception::class)
    fun composedVariableTest() {
        val shellContent = """VAR=${'$'}{0##*/}
                            |VAR=${'$'}{VAR%.*}
                            |""".trimMargin()

        val expectedPsiTree = """ASTWrapperPsiElement(FILE)
|  [PSI] Simple command
|    [PSI] Bash var def
|      PsiElement([Bash] assignment_word)('VAR')
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
|      PsiElement([Bash] assignment_word)('VAR')
|      PsiElement([Bash] =)('=')
|      [PSI] bash combined word
|        [PSI] BashComposedVar
|          PsiElement([Bash] ${'$'})('${'$'}')
|          PsiElement([Bash] {)('{')
|          [PSI] Bash-var
|            PsiElement([Bash] word)('VAR')
|          PsiElement([Bash] Parameter expansion operator '%')('%')
|          PsiElement([Bash] word)('.*')
|          PsiElement([Bash] })('}')
|  PsiElement([Bash] linefeed)('\n')
|""".trimMargin()

        assertPsiTree(shellContent, expectedPsiTree)
    }
}