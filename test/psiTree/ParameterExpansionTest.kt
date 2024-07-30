package psiTree

import org.junit.jupiter.api.Test


class ParameterExpansionTest : AbstractBashPsiTreeTest() {

  @Test
  fun `parameter expansion with dollar enclosed in single quotes and containing '#' and '*' parameter expansions`() {
    val shellCommand = """array=(a b c)   
                                |typeset array['$'{#array[*]}]="d"
                                |""".trimMargin()

    val expectedAstNodeString: String = """ASTWrapperPsiElement(FILE)
                                          |  [PSI] Simple command
                                          |    [PSI] Bash var def
                                          |      PsiElement([Bash] assignment_word)('array')
                                          |      PsiElement([Bash] =)('=')
                                          |      [PSI] assignment list
                                          |        PsiElement([Bash] ()('(')
                                          |        [PSI] bash combined word
                                          |          PsiElement([Bash] word)('a')
                                          |        PsiWhiteSpace(' ')
                                          |        [PSI] bash combined word
                                          |          PsiElement([Bash] word)('b')
                                          |        PsiWhiteSpace(' ')
                                          |        [PSI] bash combined word
                                          |          PsiElement([Bash] word)('c')
                                          |        PsiElement([Bash] ))(')')
                                          |  PsiWhiteSpace(' ')
                                          |  PsiWhiteSpace(' ')
                                          |  PsiWhiteSpace(' ')
                                          |  PsiElement([Bash] linefeed)('\n')
                                          |  [PSI] Simple command
                                          |    [PSI] BashGenericCommand
                                          |      PsiElement([Bash] word)('typeset')
                                          |    PsiWhiteSpace(' ')
                                          |    [PSI] Bash var def
                                          |      PsiElement([Bash] assignment_word)('array')
                                          |      [PSI] Arithmetic command
                                          |        PsiElement([Bash] [ (left square))('[')
                                          |        [PSI] ArithSimpleExpr
                                          |          [PSI] BashComposedVar
                                          |            PsiElement([Bash] ${'$'})(''${'$'}'')
                                          |            [PSI] Parameter expansion
                                          |              PsiElement([Bash] {)('{')
                                          |              PsiElement([Bash] Parameter expansion operator '#')('#')
                                          |              [PSI] Bash-var
                                          |                PsiElement([Bash] assignment_word)('array')
                                          |              PsiElement([Bash] [ (left square))('[')
                                          |              PsiElement([Bash] Parameter expansion operator '*')('*')
                                          |              PsiElement([Bash] ] (right square))(']')
                                          |              PsiElement([Bash] })('}')
                                          |        PsiElement([Bash] ] (right square))(']')
                                          |      PsiElement([Bash] =)('=')
                                          |      [PSI] Bash string
                                          |        PsiElement([Bash] string begin)('"')
                                          |        PsiElement([Bash] string content)('d')
                                          |        PsiElement([Bash] string end)('"')
                                          |  PsiElement([Bash] linefeed)('\n')
                                          |""".trimMargin()

    assertPsiTree(shellCommand, expectedAstNodeString)
  }

  @Test
  fun `parameter expansion in array's Index`() {
    val shellCommand = """array=(a b c) 
                                |array_pos=1
                                |array[${'$'}{array_pos}]="replaced_val"
                                |""".trimMargin()

    val expectedAstNodeString: String = """ASTWrapperPsiElement(FILE)
                                          |  [PSI] Simple command
                                          |    [PSI] Bash var def
                                          |      PsiElement([Bash] assignment_word)('array')
                                          |      PsiElement([Bash] =)('=')
                                          |      [PSI] assignment list
                                          |        PsiElement([Bash] ()('(')
                                          |        [PSI] bash combined word
                                          |          PsiElement([Bash] word)('a')
                                          |        PsiWhiteSpace(' ')
                                          |        [PSI] bash combined word
                                          |          PsiElement([Bash] word)('b')
                                          |        PsiWhiteSpace(' ')
                                          |        [PSI] bash combined word
                                          |          PsiElement([Bash] word)('c')
                                          |        PsiElement([Bash] ))(')')
                                          |  PsiWhiteSpace(' ')
                                          |  PsiElement([Bash] linefeed)('\n')
                                          |  [PSI] Simple command
                                          |    [PSI] Bash var def
                                          |      PsiElement([Bash] assignment_word)('array_pos')
                                          |      PsiElement([Bash] =)('=')
                                          |      [PSI] bash combined word
                                          |        PsiElement([Bash] int literal)('1')
                                          |  PsiElement([Bash] linefeed)('\n')
                                          |  [PSI] Simple command
                                          |    [PSI] Bash var def
                                          |      PsiElement([Bash] assignment_word)('array')
                                          |      [PSI] Arithmetic command
                                          |        PsiElement([Bash] [ (left square))('[')
                                          |        [PSI] ArithSimpleExpr
                                          |          [PSI] BashComposedVar
                                          |            PsiElement([Bash] ${'$'})('${'$'}')
                                          |            [PSI] Parameter expansion
                                          |              PsiElement([Bash] {)('{')
                                          |              [PSI] Bash-var
                                          |                PsiElement([Bash] word)('array_pos')
                                          |              PsiElement([Bash] })('}')
                                          |        PsiElement([Bash] ] (right square))(']')
                                          |      PsiElement([Bash] =)('=')
                                          |      [PSI] Bash string
                                          |        PsiElement([Bash] string begin)('"')
                                          |        PsiElement([Bash] string content)('replaced_val')
                                          |        PsiElement([Bash] string end)('"')
                                          |  PsiElement([Bash] linefeed)('\n')
                                          |""".trimMargin()

    assertPsiTree(shellCommand, expectedAstNodeString)
  }

  @Test
  fun `parameter expansion in assignment word `() {
    val shellCommand = """assignment_variable="old_value" 
                                |param=variable 
                                |assignment_${'$'}{param}="new_value"
                                |""".trimMargin()

    val expectedAstNodeString: String = """ASTWrapperPsiElement(FILE)
                                          |  [PSI] Simple command
                                          |    [PSI] Bash var def
                                          |      PsiElement([Bash] assignment_word)('assignment_variable')
                                          |      PsiElement([Bash] =)('=')
                                          |      [PSI] Bash string
                                          |        PsiElement([Bash] string begin)('"')
                                          |        PsiElement([Bash] string content)('old_value')
                                          |        PsiElement([Bash] string end)('"')
                                          |  PsiWhiteSpace(' ')
                                          |  PsiElement([Bash] linefeed)('\n')
                                          |  [PSI] Simple command
                                          |    [PSI] Bash var def
                                          |      PsiElement([Bash] assignment_word)('param')
                                          |      PsiElement([Bash] =)('=')
                                          |      [PSI] bash combined word
                                          |        PsiElement([Bash] word)('variable')
                                          |  PsiWhiteSpace(' ')
                                          |  PsiElement([Bash] linefeed)('\n')
                                          |  [PSI] Simple command
                                          |    [PSI] Bash var def
                                          |      PsiElement([Bash] assignment_word)('assignment_${'$'}{param}')
                                          |      PsiElement([Bash] =)('=')
                                          |      [PSI] Bash string
                                          |        PsiElement([Bash] string begin)('"')
                                          |        PsiElement([Bash] string content)('new_value')
                                          |        PsiElement([Bash] string end)('"')
                                          |  PsiElement([Bash] linefeed)('\n')
                                          |""".trimMargin()

    assertPsiTree(shellCommand, expectedAstNodeString)
  }

    @Test
    fun `variable used inside a parameter expansion with the colon equals operator`() {
        val bashCommand = "a=\${b:=\${c}}"

        val expectedASTNodeString: String = """ASTWrapperPsiElement(FILE)
                                         |  [PSI] Simple command
                                         |    [PSI] Bash var def
                                         |      PsiElement([Bash] assignment_word)('a')
                                         |      PsiElement([Bash] =)('=')
                                         |      [PSI] bash combined word
                                         |        [PSI] BashComposedVar
                                         |          PsiElement([Bash] ${'$'})('${'$'}')
                                         |          PsiElement([Bash] {)('{')
                                         |          [PSI] Bash var def
                                         |            PsiElement([Bash] word)('b')
                                         |          PsiElement([Bash] Parameter expansion operator ':=')(':=')
                                         |          [PSI] Bash-var
                                         |            PsiElement([Bash] variable)('${'$'}{c}')
                                         |          PsiElement([Bash] })('}')
                                         |""".trimMargin()

        assertPsiTree(bashCommand, expectedASTNodeString)
    }

    @Test
    fun `multiple variables used inside a parameter expansion with the colon equals operator`() {
        val bashCommand = "c=\${d:=\${a}+\${b}}"

        val expectedASTNodeString: String = """ASTWrapperPsiElement(FILE)
                                          |  [PSI] Simple command
                                          |    [PSI] Bash var def
                                          |      PsiElement([Bash] assignment_word)('c')
                                          |      PsiElement([Bash] =)('=')
                                          |      [PSI] bash combined word
                                          |        [PSI] BashComposedVar
                                          |          PsiElement([Bash] ${'$'})('${'$'}')
                                          |          PsiElement([Bash] {)('{')
                                          |          [PSI] Bash var def
                                          |            PsiElement([Bash] word)('d')
                                          |          PsiElement([Bash] Parameter expansion operator ':=')(':=')
                                          |          [PSI] Bash-var
                                          |            PsiElement([Bash] variable)('${'$'}{a}')
                                          |          PsiElement([Bash] word)('+')
                                          |          [PSI] Bash-var
                                          |            PsiElement([Bash] variable)('${'$'}{b}')
                                          |          PsiElement([Bash] })('}')
                                          |""".trimMargin()

        assertPsiTree(bashCommand, expectedASTNodeString)
    }

}