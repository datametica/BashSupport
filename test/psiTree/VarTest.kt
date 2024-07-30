package psiTree

import org.junit.jupiter.api.Test
import java.io.File

class VarTest: AbstractBashPsiTreeTest() {

    @Test
    @Throws(Exception::class)
    fun testVar() {
        assertPsiTree("\$foo", File("testResources/psiTree/var/var.txt").readText())
    }

    @Test
    @Throws(Exception::class)
    fun testPrintfVar() {
        assertPsiTree("printf -v foo 'test'", File("testResources/psiTree/var/printfVar.txt").readText())
    }

    @Test
    @Throws(Exception::class)
    fun testPrintfInterpolationStringVar() {
        assertPsiTree("printf -v \"\${foo}\" 'test'", File("testResources/psiTree/var/printfInterpolationStringVar.txt").readText())
    }

    @Test
    @Throws(Exception::class)
    fun testPrintfStringVar() {
        assertPsiTree("printf -v \"foo\" 'test'", File("testResources/psiTree/var/printfStringVar.txt").readText())
    }

    @Test
    @Throws(Exception::class)
    fun testPrintfString2Var() {
        assertPsiTree("printf -v \'foo\' 'test'", File("testResources/psiTree/var/printfString2Var.txt").readText())
    }

    @Test
    @Throws(Exception::class)
    fun testPrintfVarConcatenated() {
        // bash treats "foo""bar" as a single variable name "foobar", we can't support that at the moment
        // BashSupport must not parse this into two separate variable definitions
        assertPsiTree("printf -v \"foo\"\"bar\" 'test'", File("testResources/psiTree/var/printfStringConcatenated.txt").readText())
    }

}