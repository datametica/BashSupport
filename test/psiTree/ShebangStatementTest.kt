package psiTree

import org.junit.jupiter.api.Test
import java.io.File

class ShebangStatementTest: AbstractBashPsiTreeTest() {

    @Test
    fun shebang1() {
        val shellCommand = File("testResources/psiTree/expectedPsiTree/shebang.txt").readText()

        assertPsiTree(shellCommand, File("testResources/psiTree/scripts/shebang").readText())
    }
}