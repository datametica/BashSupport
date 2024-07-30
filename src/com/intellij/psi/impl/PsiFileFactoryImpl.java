package com.intellij.psi.impl;

import com.ansorgit.plugins.bash.lang.BashLanguage;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.ParserDefinition;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiPlainTextFileImpl;
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.LocalTimeCounter;
import com.intellij.util.text.CharSequenceSubSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PsiFileFactoryImpl extends PsiFileFactory {
    private static final Logger LOG = Logger.getInstance("#com.intellij.psi.impl.PsiFileFactoryImpl");

    @NotNull
    public PsiFile createFileFromText(@NotNull String name, @NotNull FileType fileType, @NotNull CharSequence text, long modificationStamp, boolean eventSystemEnabled) {
        return this.createFileFromText(name, fileType, text, modificationStamp, eventSystemEnabled, true);
    }

    public PsiFile createFileFromText(@NotNull String name, @NotNull Language language, @NotNull CharSequence text) {
        return this.createFileFromText(name, language, text, true, true);
    }

    public PsiFile createFileFromText(@NotNull String name, @NotNull Language language, @NotNull CharSequence text, boolean eventSystemEnabled, boolean markAsCopy) {
        return this.createFileFromText(name, language, text, eventSystemEnabled, markAsCopy, false);
    }

    public PsiFile createFileFromText(@NotNull String name, @NotNull Language language, @NotNull CharSequence text, boolean eventSystemEnabled, boolean markAsCopy, boolean noSizeLimit) {
        return this.createFileFromText(name, language, text, eventSystemEnabled, markAsCopy, noSizeLimit, (VirtualFile)null);
    }

    public PsiFile createFileFromText(@NotNull String name, @NotNull Language language, @NotNull CharSequence text, boolean eventSystemEnabled, boolean markAsCopy, boolean noSizeLimit, @Nullable VirtualFile original) {
        LightVirtualFile virtualFile = new LightVirtualFile(name, language, text);
        if (original != null) {
            virtualFile.setOriginalFile(original);
        }

        if (noSizeLimit) {
            SingleRootFileViewProvider.doNotCheckFileSizeLimit(virtualFile);
        }

        return this.trySetupPsiForFile(virtualFile, language, eventSystemEnabled, markAsCopy);
    }

    @NotNull
    public PsiFile createFileFromText(@NotNull String name, @NotNull FileType fileType, @NotNull CharSequence text, long modificationStamp, boolean eventSystemEnabled, boolean markAsCopy) {
        LightVirtualFile virtualFile = new LightVirtualFile(name, fileType, text, modificationStamp);
        if (fileType instanceof LanguageFileType) {
            Language language = new BashLanguage();
            PsiFile file = this.trySetupPsiForFile(virtualFile, language, eventSystemEnabled, markAsCopy);
            if (file != null) {
                return file;
            }
        }

        SingleRootFileViewProvider singleRootFileViewProvider = new SingleRootFileViewProvider(virtualFile, eventSystemEnabled);
        PsiPlainTextFileImpl plainTextFile = new PsiPlainTextFileImpl(singleRootFileViewProvider);
        if (markAsCopy) {
            CodeEditUtil.setNodeGenerated(plainTextFile.getNode(), true);
        }

        return plainTextFile;
    }

    @Nullable
    public PsiFile trySetupPsiForFile(@NotNull LightVirtualFile virtualFile, @NotNull Language language, boolean physical, boolean markAsCopy) {
        //FileViewProviderFactory factory = (FileViewProviderFactory)LanguageFileViewProviders.INSTANCE.forLanguage(language);
        //FileViewProvider viewProvider = factory != null ? factory.createFileViewProvider(virtualFile, language, this.myManager, physical) : null;
        //if (viewProvider == null) {
        FileViewProvider  viewProvider = new SingleRootFileViewProvider(virtualFile, physical);
        //}

        language = ((FileViewProvider)viewProvider).getBaseLanguage();
        ParserDefinition parserDefinition = (ParserDefinition) LanguageParserDefinitions.INSTANCE.forLanguage(language);
        if (parserDefinition != null) {
            PsiFile psiFile = ((FileViewProvider)viewProvider).getPsi(language);
            if (psiFile != null) {
                if (markAsCopy) {
                    if (psiFile.getNode() == null) {
                        throw new AssertionError("No node for file " + psiFile + "; language=" + language);
                    }

                    markGenerated(psiFile);
                }

                return psiFile;
            }
        }

        return null;
    }

    @NotNull
    public PsiFile createFileFromText(@NotNull String name, @NotNull FileType fileType, Language language, @NotNull Language targetLanguage, @NotNull CharSequence text, long modificationStamp, boolean physical, boolean markAsCopy) {
        LightVirtualFile virtualFile = new LightVirtualFile(name, fileType, text, modificationStamp);
        ParserDefinition parserDefinition = (ParserDefinition)LanguageParserDefinitions.INSTANCE.forLanguage(language);
       /* FileViewProviderFactory factory = (FileViewProviderFactory)LanguageFileViewProviders.INSTANCE.forLanguage(language);
        FileViewProvider viewProvider = factory != null ? factory.createFileViewProvider(virtualFile, language, this.myManager, physical) : null;
        if (viewProvider == null) {*/
        FileViewProvider viewProvider = new SingleRootFileViewProvider(virtualFile, physical);
        //}

        if (parserDefinition != null) {
            PsiFile psiFile = ((FileViewProvider)viewProvider).getPsi(targetLanguage);
            if (psiFile != null) {
                if (markAsCopy) {
                    markGenerated(psiFile);
                }

                return psiFile;
            }
        }

        SingleRootFileViewProvider singleRootFileViewProvider = new SingleRootFileViewProvider(virtualFile, physical);
        PsiPlainTextFileImpl plainTextFile = new PsiPlainTextFileImpl(singleRootFileViewProvider);
        if (markAsCopy) {
            CodeEditUtil.setNodeGenerated(plainTextFile.getNode(), true);
        }

        return plainTextFile;
    }

    @NotNull
    public PsiFile createFileFromText(@NotNull String name, @NotNull FileType fileType, @NotNull CharSequence text) {
        return this.createFileFromText(name, fileType, text, LocalTimeCounter.currentTime(), false);
    }

    @NotNull
    public PsiFile createFileFromText(@NotNull String name, @NotNull String text) {
        FileType type = FileTypeRegistry.getInstance().getFileTypeByFileName(name);
        if (type.isBinary()) {
            throw new RuntimeException("Cannot create binary files from text: name " + name + ", file type " + type);
        } else {
            return this.createFileFromText(name, (FileType)type, text);
        }
    }

    public PsiFile createFileFromText(FileType fileType, String fileName, CharSequence chars, int startOffset, int endOffset) {
        LOG.assertTrue(!fileType.isBinary());
        CharSequence text = startOffset == 0 && endOffset == chars.length() ? chars : new CharSequenceSubSequence(chars, startOffset, endOffset);
        return this.createFileFromText(fileName, (FileType)fileType, (CharSequence)text);
    }

    @Nullable
    public PsiFile createFileFromText(@NotNull CharSequence chars, @NotNull PsiFile original) {
        PsiFile file = this.createFileFromText(original.getName(), original.getLanguage(), chars, false, true);
        if (file != null) {
            file.putUserData(ORIGINAL_FILE, original);
        }

        return file;
    }

    @Nullable
    public PsiElement createElementFromText(@Nullable String text, @NotNull Language language, @NotNull IElementType type, @Nullable PsiElement context) {
        /*if (text == null) {
            return null;
        } else {
            DummyHolder result = DummyHolderFactory.createHolder(this.myManager, language, context);
            FileElement holder = result.getTreeElement();
            ParserDefinition parserDefinition = (ParserDefinition)LanguageParserDefinitions.INSTANCE.forLanguage(language);
            if (parserDefinition == null) {
                throw new AssertionError("No parser definition for " + language);
            } else {
                Project project = this.myManager.getProject();
                Lexer lexer = parserDefinition.createLexer(project);
                PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(project, holder, lexer, language, text);
                ASTNode node = parserDefinition.createParser(project).parse(type, builder);
                holder.rawAddChildren((TreeElement)node);
                markGenerated(result);
                return node.getPsi();
            }
        }*/
        return null;
    }

    public static void markGenerated(PsiElement element) {
        TreeElement node = (TreeElement)element.getNode();

        assert node != null;

        node.acceptTree(new GeneratedMarkerVisitor());
    }
}
