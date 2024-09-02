package psiTree

import org.junit.jupiter.api.Test
import java.io.File
import java.io.IOException

class ArithmeticExprTest: AbstractBashPsiTreeTest() {

    @Test
    @Throws(Exception::class)
    fun testShiftLeft() {
        assertPsiTree("$((1024 << 1))", File("testResources/psiTree/arithmetic/shiftLeft.txt").readText())
    }

    @Test
    @Throws(Exception::class)
    fun testShiftLeftAssignment() {
        assertPsiTree("$((a <<= 1))", File("testResources/psiTree/arithmetic/shiftLeftAssignment.txt").readText())
    }

    @Test
    @Throws(IOException::class)
    fun testShiftRight() {
        assertPsiTree("$((1024 >> 1))", File("testResources/psiTree/arithmetic/shiftRight.txt").readText())
    }

    @Test
    @Throws(IOException::class)
    fun testShiftRightAssignment() {
        assertPsiTree("$((a >>= 1))", File("testResources/psiTree/arithmetic/shiftRightAssignment.txt").readText())
    }

    @Test
    @Throws(IOException::class)
    fun testArithmetic() {
        val shellContent = """l=12
                             |echo "l (initial value l=12) = ${"$"}l"   # 12
                             |(( --l ))
                             |echo "l (after --l) = ${"$"}l"   # 11
                             |
                             |echo "m (initial value m="") = ${"$"}m"   # ""
                             |(( m-- ))
                             |echo "m (after m--) = ${"$"}m"   # -1
                             |""".trimMargin()

        assertPsiTree(shellContent, File("testResources/psiTree/arithmetic/expectedArithmeticExpr.txt").readText())
    }

    @Test
    @Throws(IOException::class)
    fun testVariableOperator() {
        val shellContent = File("testResources/psiTree/scripts/variableOperator.txt").readText()
        assertPsiTree(shellContent, File("testResources/psiTree/expectedPsiTree/variableOperator.txt").readText())
    }

    @Test
    fun arithmeticCompoundComparison() {
        val shellCommand = File("testResources/psiTree/scripts/arithmeticCompoundComparison.txt").readText()

        assertPsiTree(shellCommand, File("testResources/psiTree/expectedPsiTree/arithmeticCompoundComparison.txt").readText())
    }
}