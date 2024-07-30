/*
 * Copyright (c) Joachim Ansorg, mail@ansorg-it.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ansorgit.plugins.bash.lang.psi.util;

import com.ansorgit.plugins.bash.file.BashFileType;
import com.ansorgit.plugins.bash.lang.psi.BashVisitor;
import com.ansorgit.plugins.bash.lang.psi.api.BashString;
import com.ansorgit.plugins.bash.lang.psi.api.command.BashGenericCommand;
import com.ansorgit.plugins.bash.lang.psi.api.command.BashIncludeCommand;
import com.ansorgit.plugins.bash.lang.psi.api.heredoc.BashHereDoc;
import com.ansorgit.plugins.bash.lang.psi.api.heredoc.BashHereDocEndMarker;
import com.ansorgit.plugins.bash.lang.psi.api.heredoc.BashHereDocStartMarker;
import com.ansorgit.plugins.bash.lang.psi.api.vars.BashComposedVar;
import com.ansorgit.plugins.bash.lang.psi.api.vars.BashVar;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author jansorg
 */
public class BashPsiElementFactory {
    private static final String TEMP_FILE_NAME = "__.sh";

    public static PsiFile createDummyBashFile(String text) {
        return createFileFromText(TEMP_FILE_NAME, BashFileType.BASH_FILE_TYPE, text);
    }

    public static PsiElement createFileReference(String content) {
        PsiElement firstChild = createDummyBashFile(". " + content).getFirstChild();

        return ((BashIncludeCommand) firstChild).getFileReference();
    }

    public static PsiElement createSymbol(String name) {
        final PsiElement functionElement = createDummyBashFile(name + "() { x; }");
        return functionElement.getFirstChild().getFirstChild();
    }

    public static PsiElement createWord(String name) {
        return createDummyBashFile(name).getFirstChild().getFirstChild().getFirstChild();
    }

    public static BashGenericCommand createCommand(String command) {
        return (BashGenericCommand) createDummyBashFile(command).getFirstChild().getFirstChild();
    }

    public static BashString createString(String content) {
        String fileContent = content.startsWith("\"") && content.endsWith("\"") ? content : ("\"" + content + "\"");
        return PsiTreeUtil.findChildOfType(createDummyBashFile(fileContent), BashString.class);
    }

    public static PsiElement createAssignmentWord(String name) {
        final PsiElement assignmentCommand = createDummyBashFile(name + "=a").getFirstChild();

        return assignmentCommand.getFirstChild().getFirstChild();
    }

    public static PsiElement createVariable(String name, boolean withBraces) {
        if (withBraces) {
            final PsiElement[] result = new PsiElement[1];

            BashPsiUtils.visitRecursively(createComposedVar(name), new BashVisitor() {
                @Override
                public void visitVarUse(BashVar var) {
                    result[0] = var;
                }
            });

            return result[0];
        }

        String text = "$" + name;
        PsiElement command = createDummyBashFile(text).getFirstChild().getFirstChild();

        return command.getFirstChild().getFirstChild();
    }

    public static PsiElement createComposedVar(String varName) {
        String text = "${" + varName + "}";
        PsiElement command = createDummyBashFile(text).getFirstChild().getFirstChild();

        final PsiElement[] result = new PsiElement[1];

        BashPsiUtils.visitRecursively(command, new BashVisitor() {
            @Override
            public void visitComposedVariable(BashComposedVar composedVar) {
                result[0] = composedVar;
            }
        });

        return result[0];
    }

    public static PsiElement createShebang(String command, boolean addNewline) {
        String text = "#!" + command + (addNewline ? "\n" : "");
        return createDummyBashFile(text).getFirstChild();
    }

    public static PsiElement createNewline() {
        String text = "\n";
        return createDummyBashFile(text).getFirstChild();
    }

    public static PsiComment createComment(String comment) {
        String text = "#" + comment + "\n";

        PsiFile file = createDummyBashFile(text);
        return PsiTreeUtil.getChildOfType(file, PsiComment.class);
    }

    public static PsiElement createHeredocStartMarker(String name) {
        String data = String.format("cat << %s\n%s", name, name);
        return PsiTreeUtil.findChildOfType(createDummyBashFile(data), BashHereDocStartMarker.class);
    }

    public static PsiElement createHeredocEndMarker( String name, int leadingTabs) {
        String data = String.format("cat <<- %s\n%s", name, StringUtils.repeat("\t", leadingTabs) + name);
        return PsiTreeUtil.findChildOfType(createDummyBashFile(data), BashHereDocEndMarker.class);
    }

    public static PsiElement createHeredocContent( String content) {
        String markerName = "_BASH_EOF_";

        String data = String.format("cat << %s\n%s\n%s", markerName, content, markerName);
        return PsiTreeUtil.findChildOfType(createDummyBashFile(data), BashHereDoc.class);
    }

    @NotNull
    private static PsiFile createFileFromText(@NotNull final String name, @NotNull final FileType fileType, @NotNull final String text) {
        return new PsiFileFactoryImpl().createFileFromText(name, fileType, text);
    }
}
