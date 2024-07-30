package psiTree

import org.junit.jupiter.api.Test
import java.io.File

class IfStatementTest: AbstractBashPsiTreeTest() {

    @Test
    @Throws(Exception::class)
    fun testIf() {
        val shellContent = """if [[ 1 -eq 2 ]]
                            |then
                            |  echo "if block"
                            |elif [[ 1 -eq 1 ]]
                            |then
                            |  echo "elif block"
                            |else
                            |  echo "else block"
                            |fi""".trimMargin()

        val expectedPsiTree = """ASTWrapperPsiElement(FILE)
|  [PSI] PsiElement(if shellcommand)
|    PsiElement([Bash] if)('if')
|    PsiWhiteSpace(' ')
|    [PSI] PsiElement(extended conditional shellcommand)
|      PsiElement([Bash] [[ (left bracket))('[[ ')
|      [PSI] bash combined word
|        PsiElement([Bash] word)('1')
|      PsiWhiteSpace(' ')
|      PsiElement([Bash] cond_op)('-eq')
|      PsiWhiteSpace(' ')
|      [PSI] bash combined word
|        PsiElement([Bash] word)('2')
|      PsiElement([Bash] ]] (right bracket))(' ]]')
|    PsiElement([Bash] linefeed)('\n')
|    PsiElement([Bash] then)('then')
|    [PSI] PsiElement(logical block)
|      PsiElement([Bash] linefeed)('\n')
|      PsiWhiteSpace(' ')
|      PsiWhiteSpace(' ')
|      [PSI] Simple command
|        [PSI] BashGenericCommand
|          [PSI] bash combined word
|            PsiElement([Bash] word)('echo')
|        PsiWhiteSpace(' ')
|        [PSI] Bash string
|          PsiElement([Bash] string begin)('"')
|          PsiElement([Bash] string content)('if block')
|          PsiElement([Bash] string end)('"')
|    PsiElement([Bash] linefeed)('\n')
|    PsiElement([Bash] elif)('elif')
|    PsiWhiteSpace(' ')
|    [PSI] PsiElement(extended conditional shellcommand)
|      PsiElement([Bash] [[ (left bracket))('[[ ')
|      [PSI] bash combined word
|        PsiElement([Bash] word)('1')
|      PsiWhiteSpace(' ')
|      PsiElement([Bash] cond_op)('-eq')
|      PsiWhiteSpace(' ')
|      [PSI] bash combined word
|        PsiElement([Bash] word)('1')
|      PsiElement([Bash] ]] (right bracket))(' ]]')
|    PsiElement([Bash] linefeed)('\n')
|    PsiElement([Bash] then)('then')
|    [PSI] PsiElement(logical block)
|      PsiElement([Bash] linefeed)('\n')
|      PsiWhiteSpace(' ')
|      PsiWhiteSpace(' ')
|      [PSI] Simple command
|        [PSI] BashGenericCommand
|          [PSI] bash combined word
|            PsiElement([Bash] word)('echo')
|        PsiWhiteSpace(' ')
|        [PSI] Bash string
|          PsiElement([Bash] string begin)('"')
|          PsiElement([Bash] string content)('elif block')
|          PsiElement([Bash] string end)('"')
|    PsiElement([Bash] linefeed)('\n')
|    PsiElement([Bash] else)('else')
|    [PSI] PsiElement(logical block)
|      PsiElement([Bash] linefeed)('\n')
|      PsiWhiteSpace(' ')
|      PsiWhiteSpace(' ')
|      [PSI] Simple command
|        [PSI] BashGenericCommand
|          [PSI] bash combined word
|            PsiElement([Bash] word)('echo')
|        PsiWhiteSpace(' ')
|        [PSI] Bash string
|          PsiElement([Bash] string begin)('"')
|          PsiElement([Bash] string content)('else block')
|          PsiElement([Bash] string end)('"')
|    PsiElement([Bash] linefeed)('\n')
|    PsiElement([Bash] fi)('fi')
|""".trimMargin()
        assertPsiTree(shellContent, expectedPsiTree)
    }

    @Test
    fun simpleIfStatementWithEndifTest() {
        val shellCommand = """if [[ 1 -eq 1 ]]
                         |then
                         |  echo "condition is true"
                         |endif""".trimMargin()

        val expectedASTNodeString: String = """ASTWrapperPsiElement(FILE)
                                              |  [PSI] PsiElement(if shellcommand)
                                              |    PsiElement([Bash] if)('if')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] PsiElement(extended conditional shellcommand)
                                              |      PsiElement([Bash] [[ (left bracket))('[[ ')
                                              |      [PSI] bash combined word
                                              |        PsiElement([Bash] word)('1')
                                              |      PsiWhiteSpace(' ')
                                              |      PsiElement([Bash] cond_op)('-eq')
                                              |      PsiWhiteSpace(' ')
                                              |      [PSI] bash combined word
                                              |        PsiElement([Bash] word)('1')
                                              |      PsiElement([Bash] ]] (right bracket))(' ]]')
                                              |    PsiElement([Bash] linefeed)('\n')
                                              |    PsiElement([Bash] then)('then')
                                              |    [PSI] PsiElement(logical block)
                                              |      PsiElement([Bash] linefeed)('\n')
                                              |      PsiWhiteSpace(' ')
                                              |      PsiWhiteSpace(' ')
                                              |      [PSI] Simple command
                                              |        [PSI] BashGenericCommand
                                              |          [PSI] bash combined word
                                              |            PsiElement([Bash] word)('echo')
                                              |        PsiWhiteSpace(' ')
                                              |        [PSI] Bash string
                                              |          PsiElement([Bash] string begin)('"')
                                              |          PsiElement([Bash] string content)('condition is true')
                                              |          PsiElement([Bash] string end)('"')
                                              |    PsiElement([Bash] linefeed)('\n')
                                              |    PsiElement([Bash] endif)('endif')
                                              |""".trimMargin()

        assertPsiTree(shellCommand, expectedASTNodeString)
    }

    @Test
    fun ifStatementWithEndifTest() {
        val shellCommand = """if [[ 1 -eq 2 ]]
                         |then
                         |  echo "if block"
                         |elif [[ 1 -eq 1 ]]
                         |then
                         |  echo "elif block"
                         |else
                         |  echo "else block"
                         |endif""".trimMargin()

        val expectedASTNodeString: String = File("testResources/psiTree/scripts/expectedEndIfPsiTree.txt").readText()

        assertPsiTree(shellCommand, expectedASTNodeString)
    }

    @Test
    fun nestedIfStatementWithEndifTest() {
        val shellCommand = """if [[ 1 -eq 1 ]]
                            |then
                            |  if [[ 2 -eq 2 ]]
                            |  then
                            |    echo "echoed from nested if"
                            |  endif
                            |endif""".trimMargin()

        val expectedASTNodeString: String = """ASTWrapperPsiElement(FILE)
                                              |  [PSI] PsiElement(if shellcommand)
                                              |    PsiElement([Bash] if)('if')
                                              |    PsiWhiteSpace(' ')
                                              |    [PSI] PsiElement(extended conditional shellcommand)
                                              |      PsiElement([Bash] [[ (left bracket))('[[ ')
                                              |      [PSI] bash combined word
                                              |        PsiElement([Bash] word)('1')
                                              |      PsiWhiteSpace(' ')
                                              |      PsiElement([Bash] cond_op)('-eq')
                                              |      PsiWhiteSpace(' ')
                                              |      [PSI] bash combined word
                                              |        PsiElement([Bash] word)('1')
                                              |      PsiElement([Bash] ]] (right bracket))(' ]]')
                                              |    PsiElement([Bash] linefeed)('\n')
                                              |    PsiElement([Bash] then)('then')
                                              |    [PSI] PsiElement(logical block)
                                              |      PsiElement([Bash] linefeed)('\n')
                                              |      PsiWhiteSpace(' ')
                                              |      PsiWhiteSpace(' ')
                                              |      [PSI] PsiElement(if shellcommand)
                                              |        PsiElement([Bash] if)('if')
                                              |        PsiWhiteSpace(' ')
                                              |        [PSI] PsiElement(extended conditional shellcommand)
                                              |          PsiElement([Bash] [[ (left bracket))('[[ ')
                                              |          [PSI] bash combined word
                                              |            PsiElement([Bash] word)('2')
                                              |          PsiWhiteSpace(' ')
                                              |          PsiElement([Bash] cond_op)('-eq')
                                              |          PsiWhiteSpace(' ')
                                              |          [PSI] bash combined word
                                              |            PsiElement([Bash] word)('2')
                                              |          PsiElement([Bash] ]] (right bracket))(' ]]')
                                              |        PsiElement([Bash] linefeed)('\n')
                                              |        PsiWhiteSpace(' ')
                                              |        PsiWhiteSpace(' ')
                                              |        PsiElement([Bash] then)('then')
                                              |        [PSI] PsiElement(logical block)
                                              |          PsiElement([Bash] linefeed)('\n')
                                              |          PsiWhiteSpace(' ')
                                              |          PsiWhiteSpace(' ')
                                              |          PsiWhiteSpace(' ')
                                              |          PsiWhiteSpace(' ')
                                              |          [PSI] Simple command
                                              |            [PSI] BashGenericCommand
                                              |              [PSI] bash combined word
                                              |                PsiElement([Bash] word)('echo')
                                              |            PsiWhiteSpace(' ')
                                              |            [PSI] Bash string
                                              |              PsiElement([Bash] string begin)('"')
                                              |              PsiElement([Bash] string content)('echoed from nested if')
                                              |              PsiElement([Bash] string end)('"')
                                              |        PsiElement([Bash] linefeed)('\n')
                                              |        PsiWhiteSpace(' ')
                                              |        PsiWhiteSpace(' ')
                                              |        PsiElement([Bash] endif)('endif')
                                              |    PsiElement([Bash] linefeed)('\n')
                                              |    PsiElement([Bash] endif)('endif')
                                              |""".trimMargin()

        assertPsiTree(shellCommand, expectedASTNodeString)
    }

    @Test
    fun arithmeticCompoundComparison() {
        val shellCommand = File("testResources/psiTree/scripts/arithmeticCompoundComparison.txt").readText()

        assertPsiTree(shellCommand, File("testResources/psiTree/expectedPsiTree/arithmeticCompoundComparison.txt").readText())
    }

}