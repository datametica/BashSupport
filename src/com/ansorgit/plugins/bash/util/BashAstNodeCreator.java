package com.ansorgit.plugins.bash.util;

import com.ansorgit.plugins.bash.lang.parser.BashParserDefinition;
import com.ansorgit.plugins.bash.lang.psi.impl.BashFileImpl;
import com.ansorgit.plugins.bash.lang.psi.stubs.elements.BashStubFileElementType;
import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.impl.PsiBuilderImpl;
import com.intellij.psi.SingleRootFileViewProvider;
import com.intellij.psi.impl.source.tree.FileElement;

public class BashAstNodeCreator {

    public ASTNode create(String fileContent) {
        SingleRootFileViewProvider singleRootFileViewProvider = new SingleRootFileViewProvider(null, false);
        FileElement fileElement = new FileElement(new BashStubFileElementType(), fileContent);
        BashFileImpl bashImpl = new BashFileImpl(singleRootFileViewProvider);
        PsiBuilder builder =  new PsiBuilderImpl(bashImpl, new BashParserDefinition(), BashParserDefinition.createBashLexer(), fileElement, fileContent);
        return BashParserDefinition.createNewParser().parse(new BashStubFileElementType(), builder);
    }
}
