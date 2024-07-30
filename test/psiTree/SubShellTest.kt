package psiTree

import org.junit.jupiter.api.Test
import java.io.File

class SubShellTest: AbstractBashPsiTreeTest() {

    @Test
    fun subShell1() {
        val shellCommand = File("testResources/psiTree/scripts/subShell.txt").readText()

        assertPsiTree(shellCommand, File("testResources/psiTree/expectedPsiTree/subShell.txt").readText())
    }
}