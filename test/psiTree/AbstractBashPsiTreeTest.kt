package psiTree

import com.ansorgit.plugins.bash.lang.parser.BashParserDefinition
import com.ansorgit.plugins.bash.lang.psi.impl.BashFileImpl
import com.ansorgit.plugins.bash.lang.psi.stubs.elements.BashStubFileElementType
import com.intellij.lang.PsiBuilder
import com.intellij.lang.impl.PsiBuilderImpl
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.impl.source.tree.FileElement
import org.junit.jupiter.api.Assertions
import java.io.IOException

abstract class AbstractBashPsiTreeTest {

    @Throws(IOException::class)
    protected fun assertPsiTree(content: String, expectedPsiTreeContent: String) {
        val singleRootFileViewProvider = SingleRootFileViewProvider(null, false)
        val fileElement = FileElement(BashStubFileElementType(), content)
        val bashImpl = BashFileImpl(singleRootFileViewProvider)
        val builder: PsiBuilder = PsiBuilderImpl(
            bashImpl,
            BashParserDefinition(),
            BashParserDefinition.createBashLexer(),
            fileElement,
            content
        )
        val ast = BashParserDefinition.createNewParser().parse(BashStubFileElementType(), builder)
        val actualPsiTree = DebugUtil.psiToString(ast.psi, false)

        Assertions.assertEquals(expectedPsiTreeContent, actualPsiTree)
    }
}