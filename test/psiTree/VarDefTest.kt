package psiTree

import org.junit.jupiter.api.Test
import java.io.File

class VarDefTest: AbstractBashPsiTreeTest() {

    @Test
    @Throws(Exception::class)
    fun testVarDef() {
        assertPsiTree("foo=bar", File("testResources/psiTree/varDef/varDef.txt").readText())
    }

    @Test
    @Throws(Exception::class)
    fun testParseAssignmentList() {
        assertPsiTree("foo=(\${foo[@]%% (*})", File("testResources/psiTree/varDef/assignmentList.txt").readText())
    }
}