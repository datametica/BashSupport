package com.intellij.psi;

import com.ansorgit.plugins.bash.file.BashFileType;
import com.google.common.util.concurrent.Atomics;
import com.intellij.injected.editor.DocumentWindow;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.ParserDefinition;
import com.intellij.openapi.command.undo.UndoConstants;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.NonPhysicalFileSystem;
import com.intellij.openapi.vfs.PersistentFSConstants;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.psi.impl.PsiFileEx;
import com.intellij.psi.impl.PsiTreeChangeEventImpl;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.impl.source.tree.FileElement;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.reference.SoftReference;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.LocalTimeCounter;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class SingleRootFileViewProvider extends UserDataHolderBase implements FileViewProvider {
    private static final Key<Boolean> OUR_NO_SIZE_LIMIT_KEY = Key.create("no.size.limit");
    private static final Logger LOG = Logger.getInstance("#" + SingleRootFileViewProvider.class.getCanonicalName());
    private final VirtualFile myVirtualFile;
    private final boolean myEventSystemEnabled;
    private final boolean myPhysical;
    private final AtomicReference<PsiFile> myPsiFile;
    private volatile Content myContent;
    private volatile Reference<Document> myDocument;
    @NotNull
    private final Language myBaseLanguage;
    @NotNull
    private final FileType myFileType;

    public SingleRootFileViewProvider( VirtualFile virtualFile, boolean eventSystemEnabled) {
        this(virtualFile, eventSystemEnabled, BashFileType.BASH_FILE_TYPE);
    }

    public SingleRootFileViewProvider( VirtualFile virtualFile, boolean eventSystemEnabled, @NotNull FileType fileType) {
        this(virtualFile, eventSystemEnabled, BashFileType.BASH_LANGUAGE/*calcBaseLanguage(virtualFile, fileType)*/, fileType);
    }

    protected SingleRootFileViewProvider(VirtualFile virtualFile, boolean eventSystemEnabled, @NotNull Language language) {
        this(virtualFile, eventSystemEnabled, language, virtualFile.getFileType());
    }

    protected SingleRootFileViewProvider(VirtualFile virtualFile, boolean eventSystemEnabled, @NotNull Language language, @NotNull FileType type) {
        this.myPsiFile = Atomics.newReference();
        this.myVirtualFile = virtualFile;
        this.myEventSystemEnabled = eventSystemEnabled;
        this.myBaseLanguage = language;
        this.setContent(new VirtualFileContent());
        this.myPhysical = this.isEventSystemEnabled() && !(virtualFile instanceof LightVirtualFile) && !(virtualFile.getFileSystem() instanceof NonPhysicalFileSystem);
        this.myFileType = type;
    }

    @NotNull
    public Language getBaseLanguage() {
        return this.myBaseLanguage;
    }

    @NotNull
    public Set<Language> getLanguages() {
        return Collections.singleton(this.getBaseLanguage());
    }

    @Nullable
    public final PsiFile getPsi(@NotNull Language target) {
        return this.getPsiInner(target);
    }

    @NotNull
    public List<PsiFile> getAllFiles() {
        return ContainerUtil.createMaybeSingletonList(this.getPsi(this.getBaseLanguage()));
    }

    @Nullable
    protected PsiFile getPsiInner(@NotNull Language target) {
        if (target != this.getBaseLanguage()) {
            return null;
        } else {
            PsiFile psiFile = (PsiFile)this.myPsiFile.get();
            if (psiFile == null) {
                psiFile = this.createFile();
                if (psiFile == null) {
                    psiFile = PsiUtilCore.NULL_PSI_FILE;
                }

                boolean set = this.myPsiFile.compareAndSet(null, psiFile);
                if (!set && psiFile != PsiUtilCore.NULL_PSI_FILE) {
                    PsiFile alreadyCreated = (PsiFile)this.myPsiFile.get();
                    if (alreadyCreated == psiFile) {
                        LOG.error(this + ".createFile() must create new file instance but got the same: " + psiFile);
                    }

                    if (psiFile instanceof PsiFileEx) {
                        DebugUtil.startPsiModification("invalidating throw-away copy");

                        try {
                            ((PsiFileEx)psiFile).markInvalidated();
                        } finally {
                            DebugUtil.finishPsiModification();
                        }
                    }

                    psiFile = alreadyCreated;
                }
            }

            return psiFile == PsiUtilCore.NULL_PSI_FILE ? null : psiFile;
        }
    }

    public void beforeContentsSynchronized() {
    }

    public void contentsSynchronized() {
        if (this.myContent instanceof PsiFileContent) {
            this.setContent(new VirtualFileContent());
        }

        this.checkLengthConsistency();
    }

    public final void onContentReload() {
        List<PsiFile> files = this.getCachedPsiFiles();
        List<PsiTreeChangeEventImpl> events = ContainerUtil.newArrayList();
        List<PsiTreeChangeEventImpl> genericEvents = ContainerUtil.newArrayList();
        Iterator var4 = files.iterator();

        PsiFile psiFile;
        while(var4.hasNext()) {
            psiFile = (PsiFile)var4.next();
            genericEvents.add(this.createChildrenChangeEvent(psiFile, true));
            events.add(this.createChildrenChangeEvent(psiFile, false));
        }

        this.beforeContentsSynchronized();
        var4 = genericEvents.iterator();

        PsiTreeChangeEventImpl event;
        /*while(var4.hasNext()) {
            event = (PsiTreeChangeEventImpl)var4.next();
            ((PsiManagerImpl)this.getManager()).beforeChildrenChange(event);
        }*/

        var4 = events.iterator();

        /*while(var4.hasNext()) {
            event = (PsiTreeChangeEventImpl)var4.next();
            ((PsiManagerImpl)this.getManager()).beforeChildrenChange(event);
        }*/

        var4 = files.iterator();

        while(var4.hasNext()) {
            psiFile = (PsiFile)var4.next();
            if (psiFile instanceof PsiFileEx) {
                ((PsiFileEx)psiFile).onContentReload();
            }
        }

        var4 = events.iterator();

        /*while(var4.hasNext()) {
            event = (PsiTreeChangeEventImpl)var4.next();
            ((PsiManagerImpl)this.getManager()).childrenChanged(event);
        }*/

        var4 = genericEvents.iterator();

        /*while(var4.hasNext()) {
            event = (PsiTreeChangeEventImpl)var4.next();
            ((PsiManagerImpl)this.getManager()).childrenChanged(event);
        }*/

        this.contentsSynchronized();
    }

    private PsiTreeChangeEventImpl createChildrenChangeEvent(PsiFile file, boolean generic) {
        /*PsiTreeChangeEventImpl event = new PsiTreeChangeEventImpl(this.myManager);
        event.setParent(file);
        event.setFile(file);
        event.setGenericChange(generic);
        if (file instanceof PsiFileImpl && ((PsiFileImpl)file).isContentsLoaded()) {
            event.setOffset(0);
            event.setOldLength(file.getTextLength());
        }*/

        return null;
    }

    public void rootChanged(@NotNull PsiFile psiFile) {
        if (psiFile instanceof PsiFileImpl && ((PsiFileImpl)psiFile).isContentsLoaded()) {
            this.setContent(new PsiFileContent((PsiFileImpl)psiFile, LocalTimeCounter.currentTime()));
        }

    }

    public boolean isEventSystemEnabled() {
        return this.myEventSystemEnabled;
    }

    public boolean isPhysical() {
        return this.myPhysical;
    }

    public long getModificationStamp() {
        return this.getContent().getModificationStamp();
    }

    public boolean supportsIncrementalReparse(@NotNull Language rootLanguage) {
        return true;
    }

    public PsiFile getCachedPsi(@NotNull Language target) {
        PsiFile file = (PsiFile)this.myPsiFile.get();
        return file == PsiUtilCore.NULL_PSI_FILE ? null : file;
    }

    public List<PsiFile> getCachedPsiFiles() {
        return ContainerUtil.createMaybeSingletonList(this.getCachedPsi(this.myBaseLanguage));
    }

    private PsiFile createFile() {
        try {
            VirtualFile vFile = this.getVirtualFile();
            if (vFile.isDirectory()) {
                return null;
            } else if (this.isIgnored()) {
                return null;
            } else {
                /*Project project = this.myManager.getProject();*/
                if (this.isPhysical() && vFile.isInLocalFileSystem()) {
                    VirtualFile parent = vFile.getParent();
                    if (parent == null) {
                        return null;
                    }

                /*    PsiDirectory psiDir = this.getManager().findDirectory(parent);
                    if (psiDir == null) {
                       *//* FileIndexFacade indexFacade = FileIndexFacade.getInstance(project);*//*
                     *//*if (!indexFacade.isInLibrarySource(vFile) && !indexFacade.isInLibraryClasses(vFile)) {*//*
                            return null;
                        *//*}*//*
                    }*/
                }

                return this.createFile(vFile, this.myFileType);
            }
        } catch (ProcessCanceledException var6) {
            ProcessCanceledException e = var6;
            throw e;
        } catch (Throwable var7) {
            Throwable e = var7;
            LOG.error(e);
            return null;
        }
    }

    protected boolean isIgnored() {
        VirtualFile file = this.getVirtualFile();
        return !(file instanceof LightVirtualFile) && FileTypeRegistry.getInstance().isFileIgnored(file);
    }

    @Nullable
    protected PsiFile createFile(@NotNull VirtualFile file, @NotNull FileType fileType) {
        if (!fileType.isBinary() && !file.is(VFileProperty.SPECIAL)) {
            if (!isTooLargeForIntelligence(file)) {
                PsiFile psiFile = this.createFile(this.getBaseLanguage());
                if (psiFile != null) {
                    return psiFile;
                }
            }

            return null;//(PsiFile)(isTooLargeForContentLoading(file) ? new PsiLargeFileImpl((PsiManagerImpl)this.getManager(), this) : new PsiPlainTextFileImpl(this));
        } else {
            return null;//new PsiBinaryFileImpl((PsiManagerImpl)this.getManager(), this);
        }
    }

    /** @deprecated */
    @Deprecated
    public static boolean isTooLarge(@NotNull VirtualFile vFile) {
        return isTooLargeForIntelligence(vFile);
    }

    public static boolean isTooLargeForIntelligence(@NotNull VirtualFile vFile) {
        return !checkFileSizeLimit(vFile) ? false : fileSizeIsGreaterThan(vFile, (long) PersistentFSConstants.getMaxIntellisenseFileSize());
    }

    public static boolean isTooLargeForContentLoading(@NotNull VirtualFile vFile) {
        return fileSizeIsGreaterThan(vFile, PersistentFSConstants.FILE_LENGTH_TO_CACHE_THRESHOLD);
    }

    private static boolean checkFileSizeLimit(@NotNull VirtualFile vFile) {
        return !Boolean.TRUE.equals(vFile.getUserData(OUR_NO_SIZE_LIMIT_KEY));
    }

    public static void doNotCheckFileSizeLimit(@NotNull VirtualFile vFile) {
        vFile.putUserData(OUR_NO_SIZE_LIMIT_KEY, Boolean.TRUE);
    }

    public static boolean fileSizeIsGreaterThan(@NotNull VirtualFile vFile, long maxBytes) {
        if (vFile instanceof LightVirtualFile) {
            int lengthInChars = ((LightVirtualFile)vFile).getContent().length();
            if ((long)lengthInChars < maxBytes / 2L) {
                return false;
            }

            if ((long)lengthInChars > maxBytes) {
                return true;
            }
        }

        return vFile.getLength() > maxBytes;
    }

    @Nullable
    protected PsiFile createFile(@NotNull Language lang) {
        if (lang != this.getBaseLanguage()) {
            return null;
        } else {
            ParserDefinition parserDefinition = (ParserDefinition) LanguageParserDefinitions.INSTANCE.forLanguage(lang);
            return parserDefinition != null ? parserDefinition.createFile(this) : null;
        }
    }

    @NotNull
    public CharSequence getContents() {
        return this.getContent().getText();
    }

    @NotNull
    public VirtualFile getVirtualFile() {
        return this.myVirtualFile;
    }

    @Nullable
    private Document getCachedDocument() {
        Document document = (Document) SoftReference.dereference(this.myDocument);
        return document != null ? document : FileDocumentManager.getInstance().getCachedDocument(this.getVirtualFile());
    }

    public Document getDocument() {
        Document document = (Document)SoftReference.dereference(this.myDocument);
        if (document == null) {
            document = FileDocumentManager.getInstance().getDocument(this.getVirtualFile());
            this.myDocument = document == null ? null : new java.lang.ref.SoftReference(document);
        }

        return document;
    }

    public FileViewProvider clone() {
        VirtualFile origFile = this.getVirtualFile();
        LightVirtualFile copy = new LightVirtualFile(origFile.getName(), this.myFileType, this.getContents(), origFile.getCharset(), this.getModificationStamp());
        copy.setOriginalFile(origFile);
        copy.putUserData(UndoConstants.DONT_RECORD_UNDO, Boolean.TRUE);
        copy.setCharset(origFile.getCharset());
        return this.createCopy(copy);
    }

    @NotNull
    public SingleRootFileViewProvider createCopy(@NotNull VirtualFile copy) {
        return new SingleRootFileViewProvider(copy, false, this.myBaseLanguage);
    }

    public PsiReference findReferenceAt(int offset) {
        PsiFile psiFile = this.getPsi(this.getBaseLanguage());
        return findReferenceAt(psiFile, offset);
    }

    public PsiElement findElementAt(int offset, @NotNull Language language) {
        PsiFile psiFile = this.getPsi(language);
        return psiFile != null ? findElementAt(psiFile, offset) : null;
    }

    @Nullable
    public PsiReference findReferenceAt(int offset, @NotNull Language language) {
        PsiFile psiFile = this.getPsi(language);
        return psiFile != null ? findReferenceAt(psiFile, offset) : null;
    }

    @Nullable
    private static PsiReference findReferenceAt(@Nullable PsiFile psiFile, int offset) {
        if (psiFile == null) {
            return null;
        } else {
            int offsetInElement = offset;

            for(PsiElement child = psiFile.getFirstChild(); child != null; child = child.getNextSibling()) {
                int length = child.getTextLength();
                if (length > offsetInElement) {
                    return child.findReferenceAt(offsetInElement);
                }

                offsetInElement -= length;
            }

            return null;
        }
    }

    public PsiElement findElementAt(int offset) {
        return findElementAt(this.getPsi(this.getBaseLanguage()), offset);
    }

    public PsiElement findElementAt(int offset, @NotNull Class<? extends Language> lang) {
        return !ReflectionUtil.isAssignable(lang, this.getBaseLanguage().getClass()) ? null : this.findElementAt(offset);
    }

    @Nullable
    public static PsiElement findElementAt(@Nullable PsiElement psiFile, int offset) {
        if (psiFile == null) {
            return null;
        } else {
            int offsetInElement = offset;

            for(PsiElement child = psiFile.getFirstChild(); child != null; child = child.getNextSibling()) {
                int length = child.getTextLength();
                if (length > offsetInElement) {
                    return child.findElementAt(offsetInElement);
                }

                offsetInElement -= length;
            }

            return null;
        }
    }

    @NotNull
    private Content getContent() {
        return this.myContent;
    }

    private void setContent(@NotNull Content content) {
        this.myContent = content;
    }

    private void checkLengthConsistency() {
        Document document = this.getCachedDocument();
        if (!(document instanceof DocumentWindow)) {
            /*if (document == null || !((PsiDocumentManagerBase)PsiDocumentManager.getInstance(this.myManager.getProject())).getSynchronizer().isInSynchronization(document)) {
                List<FileElement> knownTreeRoots = this.getKnownTreeRoots();
                if (!knownTreeRoots.isEmpty()) {
                    int fileLength = this.myContent.getTextLength();
                    Iterator var4 = knownTreeRoots.iterator();

                    while(var4.hasNext()) {
                        FileElement fileElement = (FileElement)var4.next();
                        int nodeLength = fileElement.getTextLength();
                        if (nodeLength != fileLength) {
                            LOG.error("Inconsistent " + fileElement.getElementType() + " tree in " + this + "; nodeLength=" + nodeLength + "; fileLength=" + fileLength);
                        }
                    }

                }
            }*/
        }
    }

    @NonNls
    public String toString() {
        return this.getClass().getSimpleName() + "{myVirtualFile=" + this.myVirtualFile + ", content=" + this.getContent() + '}';
    }

    public void markInvalidated() {
        PsiFile psiFile = this.getCachedPsi(this.myBaseLanguage);
        if (psiFile instanceof PsiFileEx) {
            ((PsiFileEx)psiFile).markInvalidated();
        }

    }

    private CharSequence getLastCommittedText(Document document) {
        return /*PsiDocumentManager.getInstance(this.myManager.getProject()).getLastCommittedText(document);*/ "";
    }

    private long getLastCommittedStamp(Document document) {
        return /*PsiDocumentManager.getInstance(this.myManager.getProject()).getLastCommittedStamp(document);*/ 1;
    }

    @NotNull
    public PsiFile getStubBindingRoot() {
        PsiFile psi = this.getPsi(this.getBaseLanguage());

        assert psi != null;

        return psi;
    }

    @NotNull
    public final FileType getFileType() {
        return this.myFileType;
    }

    private class PsiFileContent implements Content {
        private final PsiFileImpl myFile;
        private volatile String myContent;
        private final long myModificationStamp;
        private final List<FileElement> myFileElementHardRefs;

        private PsiFileContent(PsiFileImpl file, long modificationStamp) {
            this.myFileElementHardRefs = new SmartList();
            this.myFile = file;
            this.myModificationStamp = modificationStamp;
            Iterator var5 = SingleRootFileViewProvider.this.getAllFiles().iterator();

            while(var5.hasNext()) {
                PsiFile aFile = (PsiFile)var5.next();
                if (aFile instanceof PsiFileImpl) {
                    this.myFileElementHardRefs.add(((PsiFileImpl)aFile).calcTreeElement());
                }
            }

        }

        public CharSequence getText() {
            String content = this.myContent;
            if (content == null) {
                this.myContent = content = (String) null;
            }

            return content;
        }

        public long getModificationStamp() {
            return this.myModificationStamp;
        }
    }

    private class VirtualFileContent implements Content {
        private VirtualFileContent() {
        }

        public CharSequence getText() {
            VirtualFile virtualFile = SingleRootFileViewProvider.this.getVirtualFile();
            Document document;
            if (virtualFile instanceof LightVirtualFile) {
                document = SingleRootFileViewProvider.this.getCachedDocument();
                return document != null ? SingleRootFileViewProvider.this.getLastCommittedText(document) : ((LightVirtualFile)virtualFile).getContent();
            } else {
                document = SingleRootFileViewProvider.this.getDocument();
                return document == null ? LoadTextUtil.loadText(virtualFile) : SingleRootFileViewProvider.this.getLastCommittedText(document);
            }
        }

        public long getModificationStamp() {
            Document document = SingleRootFileViewProvider.this.getCachedDocument();
            return document == null ? SingleRootFileViewProvider.this.getVirtualFile().getModificationStamp() : SingleRootFileViewProvider.this.getLastCommittedStamp(document);
        }

        @NonNls
        public String toString() {
            return "VirtualFileContent{size=" + SingleRootFileViewProvider.this.getVirtualFile().getLength() + "}";
        }
    }

    private interface Content {
        CharSequence getText();

        long getModificationStamp();
    }
}
