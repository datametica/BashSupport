package psiTree

import com.ansorgit.plugins.bash.util.BashAstNodeCreator
import com.intellij.psi.impl.DebugUtil
import org.junit.jupiter.api.Assertions
import java.io.IOException

abstract class AbstractBashPsiTreeTest {

    @Throws(IOException::class)
    protected fun assertPsiTree(content: String, expectedPsiTreeContent: String) {
        val ast = BashAstNodeCreator().create(content)
        val actualPsiTree = DebugUtil.psiToString(ast.psi, false)

        Assertions.assertEquals(expectedPsiTreeContent, actualPsiTree)
    }
}