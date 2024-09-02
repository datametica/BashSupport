package psiTree

import org.junit.jupiter.api.Test

class CaseStatementTest: AbstractBashPsiTreeTest() {

    @Test
    fun `case Statement With Regex In Lable`() {
        val shellCommand = """
      |case variable in 
      |(@(+([0-9])*(RT|FG|RD|TH)_ABC_FDE)) echo "Regex Pattern Matched !";
      |;;
      |esac
      |""".trimMargin()
        val expectedASTNodeString: String = """|ASTWrapperPsiElement(FILE)
                                           |  [PSI] PsiElement(case pattern)
                                           |    PsiElement([Bash] case)('case')
                                           |    PsiWhiteSpace(' ')
                                           |    [PSI] bash combined word
                                           |      PsiElement([Bash] word)('variable')
                                           |    PsiWhiteSpace(' ')
                                           |    PsiElement([Bash] in)('in')
                                           |    PsiWhiteSpace(' ')
                                           |    PsiElement([Bash] linefeed)('\n')
                                           |    [PSI] case pattern list element
                                           |      PsiElement([Bash] ()('(')
                                           |      [PSI] BashCasePattern
                                           |        [PSI] bash combined word
                                           |          PsiElement([Bash] word)('@(+([0-9])*(RT|FG|RD|TH)_ABC_FDE)')
                                           |      PsiElement([Bash] ))(')')
                                           |      PsiWhiteSpace(' ')
                                           |      [PSI] PsiElement(logical block)
                                           |        [PSI] Simple command
                                           |          [PSI] BashGenericCommand
                                           |            [PSI] bash combined word
                                           |              PsiElement([Bash] word)('echo')
                                           |          PsiWhiteSpace(' ')
                                           |          [PSI] Bash string
                                           |            PsiElement([Bash] string begin)('"')
                                           |            PsiElement([Bash] string content)('Regex Pattern Matched !')
                                           |            PsiElement([Bash] string end)('"')
                                           |      PsiElement([Bash] ;)(';')
                                           |      PsiElement([Bash] linefeed)('\n')
                                           |      PsiElement([Bash] ;;)(';;')
                                           |    PsiElement([Bash] linefeed)('\n')
                                           |    PsiElement([Bash] esac)('esac')
                                           |  PsiElement([Bash] linefeed)('\n')
                                           |""".trimMargin()
        assertPsiTree(shellCommand, expectedASTNodeString)
    }

    @Test
    @Throws(Exception::class)
    fun testCase() {
        val shellContent = """
      |case \$\x in 
      |D) export y=0 ;;
      |G) y=1 ;;
      |esac
      |""".trimMargin()

        val expectedContent = """ASTWrapperPsiElement(FILE)
|  [PSI] PsiElement(case pattern)
|    PsiElement([Bash] case)('case')
|    PsiWhiteSpace(' ')
|    [PSI] bash combined word
|      PsiElement([Bash] word)('\${'$'}\x')
|    PsiWhiteSpace(' ')
|    PsiElement([Bash] in)('in')
|    PsiWhiteSpace(' ')
|    PsiElement([Bash] linefeed)('\n')
|    [PSI] case pattern list element
|      [PSI] BashCasePattern
|        [PSI] bash combined word
|          PsiElement([Bash] word)('D')
|      PsiElement([Bash] ))(')')
|      PsiWhiteSpace(' ')
|      [PSI] PsiElement(logical block)
|        [PSI] Simple command
|          [PSI] BashGenericCommand
|            PsiElement([Bash] word)('export')
|          PsiWhiteSpace(' ')
|          [PSI] Bash var def
|            PsiElement([Bash] assignment_word)('y')
|            PsiElement([Bash] =)('=')
|            [PSI] bash combined word
|              PsiElement([Bash] int literal)('0')
|      PsiWhiteSpace(' ')
|      PsiElement([Bash] ;;)(';;')
|    PsiElement([Bash] linefeed)('\n')
|    [PSI] case pattern list element
|      [PSI] BashCasePattern
|        [PSI] bash combined word
|          PsiElement([Bash] word)('G')
|      PsiElement([Bash] ))(')')
|      PsiWhiteSpace(' ')
|      [PSI] PsiElement(logical block)
|        [PSI] Simple command
|          [PSI] Bash var def
|            PsiElement([Bash] assignment_word)('y')
|            PsiElement([Bash] =)('=')
|            [PSI] bash combined word
|              PsiElement([Bash] int literal)('1')
|      PsiWhiteSpace(' ')
|      PsiElement([Bash] ;;)(';;')
|    PsiElement([Bash] linefeed)('\n')
|    PsiElement([Bash] esac)('esac')
|  PsiElement([Bash] linefeed)('\n')
|""".trimMargin()

        assertPsiTree(shellContent, expectedContent)
    }

}