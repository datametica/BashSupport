package psiTree

import org.junit.jupiter.api.Test
import psiTree.AbstractBashPsiTreeTest

class BteqPsiTreeTest: AbstractBashPsiTreeTest() {

    @Test
    @Throws(Exception::class)
    fun bteqPsiTreeTest() {
        val shellContent = "bteq<<BTEQEOF\n" +
                "select * from foodmart.currency;\n" +
                "BTEQEOF"

        val expectedPsiTree = "ASTWrapperPsiElement(FILE)\n" +
                "  [PSI] bash composed command\n" +
                "    [PSI] Simple command\n" +
                "      [PSI] BashGenericCommand\n" +
                "        [PSI] bash combined word\n" +
                "          PsiElement([Bash] word)('bteq')\n" +
                "      [PSI] BashRedirectList\n" +
                "        PsiElement([Bash] heredoc marker tag)('<<')\n" +
                "        [PSI] Bash heredoc start marker\n" +
                "          PsiElement([Bash] heredoc start marker)('BTEQEOF')\n" +
                "    PsiElement([Bash] linefeed)('\\n')\n" +
                "    [PSI] bash here doc\n" +
                "      PsiElement([Bash] here doc content)('select * from foodmart.currency;\\n')\n" +
                "    [PSI] Bash heredoc end marker\n" +
                "      PsiElement([Bash] heredoc end marker)('BTEQEOF')\n"

        assertPsiTree(shellContent, expectedPsiTree)
    }
}