package psiTree

import org.junit.jupiter.api.Test
import java.io.File

class LoopPsiTreeTest: AbstractBashPsiTreeTest() {

    @Test
    @Throws(Exception::class)
    fun testFor1() {
        val blockText = """|for i in 1 2
                          |do
                          |  echo "Iteration no \$\i"
                          |  for t in 3 2
                          |  do
                          |   echo "Iteration no \$\t"
                          |  done
                          |done""".trimMargin()

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
|    PsiElement([Bash] linefeed)('\n')
|    PsiElement([Bash] do)('do')
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
|          PsiElement([Bash] string content)('Iteration no \${'$'}\i')
|          PsiElement([Bash] string end)('"')
|      PsiElement([Bash] linefeed)('\n')
|      PsiWhiteSpace(' ')
|      PsiWhiteSpace(' ')
|      [PSI] PsiElement(for shellcommand)
|        PsiElement([Bash] for)('for')
|        PsiWhiteSpace(' ')
|        [PSI] Bash var def
|          PsiElement([Bash] word)('t')
|        PsiWhiteSpace(' ')
|        PsiElement([Bash] in)('in')
|        PsiWhiteSpace(' ')
|        [PSI] bash combined word
|          PsiElement([Bash] int literal)('3')
|        PsiWhiteSpace(' ')
|        [PSI] bash combined word
|          PsiElement([Bash] int literal)('2')
|        PsiElement([Bash] linefeed)('\n')
|        PsiWhiteSpace(' ')
|        PsiWhiteSpace(' ')
|        PsiElement([Bash] do)('do')
|        [PSI] PsiElement(logical block)
|          PsiElement([Bash] linefeed)('\n')
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
|              PsiElement([Bash] string content)('Iteration no \${'$'}\t')
|              PsiElement([Bash] string end)('"')
|        PsiElement([Bash] linefeed)('\n')
|        PsiWhiteSpace(' ')
|        PsiWhiteSpace(' ')
|        PsiElement([Bash] done)('done')
|    PsiElement([Bash] linefeed)('\n')
|    PsiElement([Bash] done)('done')
|""".trimMargin()

        assertPsiTree(blockText, expectedASTNodeString)
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun testFor2() {
        val blockText = """|for i in 1 2
                          |do
                          |  if [ \$\i == 2 ]
                          |    then
                          |       echo "in if block, for i: \$\i"
                          |  fi
                          |  echo "after if block, for i: \$\i"
                          |done""".trimMargin()

        val expectedPsiTree = """ASTWrapperPsiElement(FILE)
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
|    PsiElement([Bash] linefeed)('\n')
|    PsiElement([Bash] do)('do')
|    [PSI] PsiElement(logical block)
|      PsiElement([Bash] linefeed)('\n')
|      PsiWhiteSpace(' ')
|      PsiWhiteSpace(' ')
|      [PSI] PsiElement(if shellcommand)
|        PsiElement([Bash] if)('if')
|        PsiWhiteSpace(' ')
|        [PSI] PsiElement(conditional shellcommand)
|          PsiElement([Bash] [ (left conditional))('[ ')
|          PsiElement([Bash] word)('\${'$'}\i')
|          PsiWhiteSpace(' ')
|          PsiElement([Bash] cond_op ==)('==')
|          PsiWhiteSpace(' ')
|          [PSI] bash combined word
|            PsiElement([Bash] word)('2')
|          PsiElement([Bash]  ] (right conditional))(' ]')
|        PsiElement([Bash] linefeed)('\n')
|        PsiWhiteSpace(' ')
|        PsiWhiteSpace(' ')
|        PsiWhiteSpace(' ')
|        PsiWhiteSpace(' ')
|        PsiElement([Bash] then)('then')
|        [PSI] PsiElement(logical block)
|          PsiElement([Bash] linefeed)('\n')
|          PsiWhiteSpace(' ')
|          PsiWhiteSpace(' ')
|          PsiWhiteSpace(' ')
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
|              PsiElement([Bash] string content)('in if block, for i: \${'$'}\i')
|              PsiElement([Bash] string end)('"')
|        PsiElement([Bash] linefeed)('\n')
|        PsiWhiteSpace(' ')
|        PsiWhiteSpace(' ')
|        PsiElement([Bash] fi)('fi')
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
|          PsiElement([Bash] string content)('after if block, for i: \${'$'}\i')
|          PsiElement([Bash] string end)('"')
|    PsiElement([Bash] linefeed)('\n')
|    PsiElement([Bash] done)('done')
|""".trimMargin()
        assertPsiTree(blockText, expectedPsiTree)
    }

    @Test
    @Throws(Exception::class)
    fun testWhile() {
        val shellContent = File("testResources/psiTree/scripts/whileLoop.txt").readText()

        val expectedPsiTree = File("testResources/psiTree/scripts/expectedWhilePsi.txt").readText()

        assertPsiTree(shellContent, expectedPsiTree)
    }

    @Test
    @Throws(Exception::class)
    fun testFor() {
        val shellContent = "for i in {1..3}\n" +
                "do\n" +
                "  echo \"Iteration no \$i\"\n" +
                "  break 1\n" +
                "done"

        val expectedPsiTree = "ASTWrapperPsiElement(FILE)\n" +
                "  [PSI] PsiElement(for shellcommand)\n" +
                "    PsiElement([Bash] for)('for')\n" +
                "    PsiWhiteSpace(' ')\n" +
                "    [PSI] Bash var def\n" +
                "      PsiElement([Bash] word)('i')\n" +
                "    PsiWhiteSpace(' ')\n" +
                "    PsiElement([Bash] in)('in')\n" +
                "    PsiWhiteSpace(' ')\n" +
                "    [PSI] bash combined word\n" +
                "      [PSI] Bash expansion\n" +
                "        PsiElement([Bash] {)('{')\n" +
                "        PsiElement([Bash] word)('1..3')\n" +
                "        PsiElement([Bash] })('}')\n" +
                "    PsiElement([Bash] linefeed)('\\n')\n" +
                "    PsiElement([Bash] do)('do')\n" +
                "    [PSI] PsiElement(logical block)\n" +
                "      PsiElement([Bash] linefeed)('\\n')\n" +
                "      PsiWhiteSpace(' ')\n" +
                "      PsiWhiteSpace(' ')\n" +
                "      [PSI] Simple command\n" +
                "        [PSI] BashGenericCommand\n" +
                "          [PSI] bash combined word\n" +
                "            PsiElement([Bash] word)('echo')\n" +
                "        PsiWhiteSpace(' ')\n" +
                "        [PSI] Bash string\n" +
                "          PsiElement([Bash] string begin)('\"')\n" +
                "          PsiElement([Bash] string content)('Iteration no ')\n" +
                "          [PSI] Bash-var\n" +
                "            PsiElement([Bash] variable)('\$i')\n" +
                "          PsiElement([Bash] string end)('\"')\n" +
                "      PsiElement([Bash] linefeed)('\\n')\n" +
                "      PsiWhiteSpace(' ')\n" +
                "      PsiWhiteSpace(' ')\n" +
                "      [PSI] Simple command\n" +
                "        [PSI] BashGenericCommand\n" +
                "          [PSI] bash combined word\n" +
                "            PsiElement([Bash] word)('break')\n" +
                "        PsiWhiteSpace(' ')\n" +
                "        [PSI] bash combined word\n" +
                "          PsiElement([Bash] int literal)('1')\n" +
                "    PsiElement([Bash] linefeed)('\\n')\n" +
                "    PsiElement([Bash] done)('done')\n"

        assertPsiTree(shellContent, expectedPsiTree)
    }


}