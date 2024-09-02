package psiTree

import org.junit.jupiter.api.Test
import java.io.File

class BitwiseOperationTest: AbstractBashPsiTreeTest() {

    @Test
    fun bitwiseOperationTest() {
        val shellCommand = File("testResources/psiTree/scripts/bitwiseOperation.txt").readText()

        assertPsiTree(shellCommand, File("testResources/psiTree/expectedPsiTree/bitwiseOperation.txt").readText())
    }
}