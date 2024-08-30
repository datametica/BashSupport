/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.psi.impl.source;

import com.intellij.extapi.psi.StubBasedPsiElementBase;
import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.lang.*;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.Factory;
import com.intellij.openapi.util.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWithId;
import com.intellij.psi.*;
import com.intellij.psi.impl.*;
import com.intellij.psi.impl.file.PsiFileImplUtil;
import com.intellij.psi.impl.file.impl.FileManagerImpl;
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil;
import com.intellij.psi.impl.source.resolve.FileContextUtil;
import com.intellij.psi.impl.source.text.BlockSupportImpl;
import com.intellij.psi.impl.source.tree.*;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.stubs.*;
import com.intellij.psi.tree.*;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.reference.SoftReference;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.PatchedWeakReference;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.util.*;

public abstract class PsiFileImpl extends ElementBase implements PsiFileEx, PsiFileWithStubSupport {
    private static final Logger LOG = Logger.getInstance("#com.intellij.psi.impl.source.PsiFileImpl");
    public static final String STUB_PSI_MISMATCH = "stub-psi mismatch";
    private IElementType myElementType;
    protected IElementType myContentElementType;
    private long myModificationStamp;
    protected PsiFile myOriginalFile;
    private final FileViewProvider myViewProvider;
    private volatile Reference<StubTree> myStub;
    private boolean myInvalidated;
    private volatile Getter<FileElement> myTreeElementPointer;
    public static final Key<Boolean> BUILDING_STUB = new Key("Don't use stubs mark!");
    private static final Comparator<PsiFile> FILE_BY_LANGUAGE_ID = new Comparator<PsiFile>() {
        public int compare(@NotNull PsiFile o1, @NotNull PsiFile o2) {
            return o1.getLanguage().getID().compareTo(o2.getLanguage().getID());
        }
    };
    private static final Key<Reference<StubTree>> STUB_TREE_IN_PARSED_TREE = Key.create("STUB_TREE_IN_PARSED_TREE");
    private final Object myStubFromTreeLock;

    protected PsiFileImpl(@NotNull IElementType elementType, IElementType contentElementType, @NotNull FileViewProvider provider) {
        this(provider);
        this.init(elementType, contentElementType);
    }

    protected PsiFileImpl(@NotNull FileViewProvider provider) {
        this.myStubFromTreeLock = new Object();
        this.myViewProvider = provider;
    }

    public void setContentElementType(IElementType contentElementType) {
        LOG.assertTrue(contentElementType instanceof ILazyParseableElementType, contentElementType);
        this.myContentElementType = contentElementType;
    }

    public IElementType getContentElementType() {
        return this.myContentElementType;
    }

    protected void init(@NotNull IElementType elementType, IElementType contentElementType) {
        this.myElementType = elementType;
        this.setContentElementType(contentElementType);
    }

    public TreeElement createContentLeafElement(CharSequence leafText) {
        return (TreeElement)(this.myContentElementType instanceof ILazyParseableElementType ? ASTFactory.lazy((ILazyParseableElementType)this.myContentElementType, leafText) : ASTFactory.leaf(this.myContentElementType, leafText));
    }

    public boolean isDirectory() {
        return false;
    }

    public FileElement getTreeElement() {
        FileElement node = this.derefTreeElement();
        if (node != null) {
            return node;
        } else {
            return !this.getViewProvider().isPhysical() ? this.loadTreeElement() : null;
        }
    }

    private FileElement derefTreeElement() {
        Getter<FileElement> pointer = this.myTreeElementPointer;
        if (pointer == null) {
            return null;
        } else {
            FileElement treeElement = (FileElement)pointer.get();
            if (treeElement != null) {
                return treeElement;
            } else {
                synchronized(PsiLock.LOCK) {
                    if (this.myTreeElementPointer == pointer) {
                        this.myTreeElementPointer = null;
                    }

                    return null;
                }
            }
        }
    }

    public VirtualFile getVirtualFile() {
        return this.getViewProvider().isEventSystemEnabled() ? this.getViewProvider().getVirtualFile() : null;
    }

    public boolean processChildren(PsiElementProcessor<PsiFileSystemItem> processor) {
        return true;
    }

    public boolean isValid() {
        if (!this.myViewProvider.getVirtualFile().isValid()) {
            return false;
        } else {
            return !this.myInvalidated;
        }
    }

    public void markInvalidated() {
        this.myInvalidated = true;
        DebugUtil.onInvalidated(this);
    }

    public boolean isContentsLoaded() {
        return this.derefTreeElement() != null;
    }

    @NotNull
    private FileElement loadTreeElement() {
        FileViewProvider viewProvider = this.getViewProvider();
        /*if (viewProvider.isPhysical() && this.myManager.isAssertOnFileLoading(viewProvider.getVirtualFile())) {
            LOG.error("Access to tree elements not allowed in tests. path='" + viewProvider.getVirtualFile().getPresentableUrl() + "'");
        }*/

        Document cachedDocument = FileDocumentManager.getInstance().getCachedDocument(this.getViewProvider().getVirtualFile());
        FileElement treeElement = this.createFileElement(viewProvider.getContents());
        treeElement.setPsi(this);

        FileElement savedTree;
        do {
            StubTree stub = this.derefStub();
            List<Pair<StubBasedPsiElementBase, CompositeElement>> bindings = this.calcStubAstBindings(treeElement, cachedDocument, stub);
            savedTree = this.ensureTreeElement(viewProvider, treeElement, stub, bindings);
        } while(savedTree == null);

        return savedTree;
    }

    @Nullable
    private FileElement ensureTreeElement(@NotNull FileViewProvider viewProvider, @NotNull FileElement treeElement, @Nullable StubTree stub, @NotNull List<Pair<StubBasedPsiElementBase, CompositeElement>> bindings) {
        synchronized(PsiLock.LOCK) {
            FileElement existing = this.derefTreeElement();
            if (existing != null) {
                return existing;
            } else if (stub != this.derefStub()) {
                return null;
            } else {
                if (stub != null) {
                    treeElement.putUserData(STUB_TREE_IN_PARSED_TREE, new SoftReference(stub));
                    this.putUserData(ObjectStubTree.LAST_STUB_TREE_HASH, stub.hashCode());
                }

                switchFromStubToAst(bindings);
                this.myStub = null;
                this.myTreeElementPointer = this.createTreeElementPointer(treeElement);
                if (LOG.isDebugEnabled() && viewProvider.isPhysical()) {
                    LOG.debug("Loaded text for file " + viewProvider.getVirtualFile().getPresentableUrl());
                }

                return treeElement;
            }
        }
    }

    public ASTNode findTreeForStub(StubTree tree, StubElement<?> stub) {
        Iterator<StubElement<?>> stubs = tree.getPlainList().iterator();
        StubElement<?> root = (StubElement)stubs.next();
        CompositeElement ast = this.calcTreeElement();
        return (ASTNode)(root == stub ? ast : findTreeForStub(ast, stubs, stub));
    }

    @Nullable
    private static ASTNode findTreeForStub(ASTNode tree, Iterator<StubElement<?>> stubs, StubElement stub) {
        IElementType type = tree.getElementType();
        if (type instanceof IStubElementType && ((IStubElementType)type).shouldCreateStub(tree)) {
            StubElement curStub = (StubElement)stubs.next();
            if (curStub == stub) {
                return tree;
            }
        }

        ASTNode[] var9 = tree.getChildren((TokenSet)null);
        int var5 = var9.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            ASTNode node = var9[var6];
            ASTNode treeForStub = findTreeForStub(node, stubs, stub);
            if (treeForStub != null) {
                return treeForStub;
            }
        }

        return null;
    }

    private static void switchFromStubToAst(List<Pair<StubBasedPsiElementBase, CompositeElement>> pairs) {
        Iterator var1 = pairs.iterator();

        while(var1.hasNext()) {
            Pair<StubBasedPsiElementBase, CompositeElement> pair = (Pair)var1.next();
            ((CompositeElement)pair.second).setPsi((PsiElement)pair.first);
            ((StubBasedPsiElementBase)pair.first).setNode((ASTNode)pair.second);
            ((StubBasedPsiElementBase)pair.first).setStub((StubElement)null);
        }

    }

    private List<Pair<StubBasedPsiElementBase, CompositeElement>> calcStubAstBindings(ASTNode root, final Document cachedDocument, final StubTree stubTree) {
        return null;
    }

    @Nullable
    public IStubFileElementType getElementTypeForStubBuilder() {
        IFileElementType type = ((ParserDefinition) LanguageParserDefinitions.INSTANCE.forLanguage(this.getLanguage())).getFileNodeType();
        return type instanceof IStubFileElementType ? (IStubFileElementType)type : null;
    }

    @NotNull
    protected FileElement createFileElement(CharSequence docText) {
        TreeElement contentLeaf = this.createContentLeafElement(docText);
        FileElement treeElement;
        if (contentLeaf instanceof FileElement) {
            treeElement = (FileElement)contentLeaf;
        } else {
            CompositeElement xxx = ASTFactory.composite(this.myElementType);

            assert xxx instanceof FileElement : "BUMM";

            treeElement = (FileElement)xxx;
            treeElement.rawAddChildrenWithoutNotifications(contentLeaf);
        }

        return treeElement;
    }

    private void clearStub(@NotNull String reason) {
        StubTree stubHolder = (StubTree)SoftReference.dereference(this.myStub);
        if (stubHolder != null) {
            ((PsiFileStubImpl)stubHolder.getRoot()).clearPsi(reason);
        }

        this.myStub = null;
    }

    public void clearCaches() {
        ++this.myModificationStamp;
    }

    public String getText() {
        ASTNode tree = this.derefTreeElement();
        if (!this.isValid()) {
            if (tree != null) {
                return tree.getText();
            } else {
                throw new PsiInvalidElementAccessException(this);
            }
        } else {
            String string = this.getViewProvider().getContents().toString();
            if (tree != null && string.length() != tree.getTextLength()) {
                throw new AssertionError("File text mismatch: tree.length=" + tree.getTextLength() + "; psi.length=" + string.length() + "; this=" + this + "; vp=" + this.getViewProvider());
            } else {
                return string;
            }
        }
    }

    public int getTextLength() {
        ASTNode tree = this.derefTreeElement();
        if (tree != null) {
            return tree.getTextLength();
        } else {
            PsiUtilCore.ensureValid(this);
            return this.getViewProvider().getContents().length();
        }
    }

    public TextRange getTextRange() {
        return new TextRange(0, this.getTextLength());
    }

    public PsiElement getNextSibling() {
        return SharedPsiElementImplUtil.getNextSibling(this);
    }

    public PsiElement getPrevSibling() {
        return SharedPsiElementImplUtil.getPrevSibling(this);
    }

    public long getModificationStamp() {
        return this.myModificationStamp;
    }

    public void subtreeChanged() {
        this.doClearCaches("subtreeChanged");
        this.getViewProvider().rootChanged(this);
    }

    private void doClearCaches(String reason) {
        FileElement tree = this.getTreeElement();
        if (tree != null) {
            tree.clearCaches();
        }

        synchronized(PsiLock.LOCK) {
            this.clearStub(reason);
        }

  /*      if (tree != null) {
            tree.putUserData(STUB_TREE_IN_PARSED_TREE, (Object)null);
        }*/

        this.clearCaches();
    }

    protected PsiFileImpl clone() {
        FileViewProvider viewProvider = this.getViewProvider();
        FileViewProvider providerCopy = viewProvider.clone();
        Language language = this.getLanguage();
        if (providerCopy == null) {
            throw new AssertionError("Unable to clone the view provider: " + viewProvider + "; " + language);
        } else {
            PsiFileImpl clone = BlockSupportImpl.getFileCopy(this, providerCopy);
            this.copyCopyableDataTo(clone);
            if (this.getTreeElement() != null) {
                FileElement treeClone = (FileElement)this.calcTreeElement().clone();
                clone.setTreeElementPointer(treeClone);
                treeClone.setPsi(clone);
            }

            if (viewProvider.isEventSystemEnabled()) {
                clone.myOriginalFile = this;
            } else if (this.myOriginalFile != null) {
                clone.myOriginalFile = this.myOriginalFile;
            }

            FileManagerImpl.clearPsiCaches(providerCopy);
            return clone;
        }
    }

    @NotNull
    public String getName() {
        return this.getViewProvider().getVirtualFile().getName();
    }

    public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
        this.checkSetName(name);
        this.doClearCaches("setName");
        return PsiFileImplUtil.setName(this, name);
    }

    public void checkSetName(String name) throws IncorrectOperationException {
        if (this.getViewProvider().isEventSystemEnabled()) {
            PsiFileImplUtil.checkSetName(this, name);
        }
    }

    public boolean isWritable() {
        return this.getViewProvider().getVirtualFile().isWritable();
    }

    public PsiDirectory getParent() {
        return this.getContainingDirectory();
    }

    @Nullable
    public PsiDirectory getContainingDirectory() {
        VirtualFile file = this.getViewProvider().getVirtualFile();
        VirtualFile parentFile = file.getParent();
        if (parentFile == null) {
            return null;
        } else if (!parentFile.isValid()) {
            LOG.error("Invalid parent: " + parentFile + " of file " + file + ", file.valid=" + file.isValid());
            return null;
        } else {
            return null;//return this.getManager().findDirectory(parentFile);
        }
    }

    @NotNull
    public PsiFile getContainingFile() {
        return this;
    }

    public void delete() throws IncorrectOperationException {
        this.checkDelete();
        PsiFileImplUtil.doDelete(this);
    }

    public void checkDelete() throws IncorrectOperationException {
        if (!this.getViewProvider().isEventSystemEnabled()) {
            throw new IncorrectOperationException();
        } else {
            CheckUtil.checkWritable(this);
        }
    }

    @NotNull
    public PsiFile getOriginalFile() {
        return (PsiFile)(this.myOriginalFile == null ? this : this.myOriginalFile);
    }

    public void setOriginalFile(@NotNull PsiFile originalFile) {
        this.myOriginalFile = originalFile.getOriginalFile();
    }

    @NotNull
    public PsiFile[] getPsiRoots() {
        FileViewProvider viewProvider = this.getViewProvider();
        Set<Language> languages = viewProvider.getLanguages();
        PsiFile[] roots = new PsiFile[languages.size()];
        int i = 0;

        PsiFile psi;
        for(Iterator var5 = languages.iterator(); var5.hasNext(); roots[i++] = psi) {
            Language language = (Language)var5.next();
            psi = viewProvider.getPsi(language);
            if (psi == null) {
                LOG.error("PSI is null for " + language + "; in file: " + this);
            }
        }

        if (roots.length > 1) {
            Arrays.sort(roots, FILE_BY_LANGUAGE_ID);
        }

        return roots;
    }

    public boolean isPhysical() {
        return this.getViewProvider().isEventSystemEnabled();
    }

    @NotNull
    public Language getLanguage() {
        return this.myElementType.getLanguage();
    }

    @NotNull
    public FileViewProvider getViewProvider() {
        return this.myViewProvider;
    }

    public void setTreeElementPointer(FileElement element) {
        this.myTreeElementPointer = element;
    }

    public PsiElement findElementAt(int offset) {
        return this.getViewProvider().findElementAt(offset);
    }

    public PsiReference findReferenceAt(int offset) {
        return this.getViewProvider().findReferenceAt(offset);
    }

    @NotNull
    public char[] textToCharArray() {
        return CharArrayUtil.fromSequence(this.getViewProvider().getContents());
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T findChildByClass(Class<T> aClass) {
        PsiElement[] var2 = this.getChildren();
        int var3 = var2.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            PsiElement child = var2[var4];
            if (aClass.isInstance(child)) {
                return (T) child;
            }
        }

        return null;
    }

    public PsiElement getContext() {
        return FileContextUtil.getFileContext(this);
    }

    public void onContentReload() {
        FileElement treeElement = this.derefTreeElement();
        DebugUtil.startPsiModification("onContentReload");

        try {
            if (treeElement != null) {
                this.myTreeElementPointer = null;
                treeElement.detachFromFile();
                DebugUtil.onInvalidated(treeElement);
            }

            this.clearStub("onContentReload");
        } finally {
            DebugUtil.finishPsiModification();
        }

        this.clearCaches();
    }

    @Nullable
    public StubElement getStub() {
        StubTree stubHolder = this.getStubTree();
        return stubHolder != null ? stubHolder.getRoot() : null;
    }

    @Nullable
    public StubTree getStubTree() {
        if (Boolean.TRUE.equals(this.getUserData(BUILDING_STUB))) {
            return null;
        } else {
            StubTree derefd = this.derefStub();
            if (derefd != null) {
                return derefd;
            } else if (this.getTreeElement() != null) {
                return null;
            } else if (this.getElementTypeForStubBuilder() == null) {
                return null;
            } else {
                VirtualFile vFile = this.getVirtualFile();
                if (!(vFile instanceof VirtualFileWithId)) {
                    return null;
                } else {
                    return null;
/*
                    ObjectStubTree tree = StubTreeLoader.getInstance().readOrBuild(this.getProject(), vFile, this);
                    if (!(tree instanceof StubTree)) {
                        return null;
                    } else {
                        StubTree stubHolder = (StubTree)tree;
                        FileViewProvider viewProvider = this.getViewProvider();
                        List<Pair<IStubFileElementType, PsiFile>> roots = StubTreeBuilder.getStubbedRoots(viewProvider);
                        synchronized(PsiLock.LOCK) {
                            if (this.getTreeElement() != null) {
                                return null;
                            } else {
                                StubTree derefdOnLock = this.derefStub();
                                if (derefdOnLock != null) {
                                    return derefdOnLock;
                                } else {
                                    PsiFileStub baseRoot = stubHolder.getRoot();
                                    if (baseRoot instanceof PsiFileStubImpl && !((PsiFileStubImpl)baseRoot).rootsAreSet()) {
                                        LOG.error("Stub roots must be set when stub tree was read or built with StubTreeLoader");
                                        return stubHolder;
                                    } else {
                                        PsiFileStub[] stubRoots = baseRoot.getStubRoots();
                                        if (stubRoots.length != roots.size()) {
                                            Function<PsiFileStub, String> stubToString = new Function<PsiFileStub, String>() {
                                                public String fun(PsiFileStub stub) {
                                                    return stub.getClass().getSimpleName();
                                                }
                                            };
                                            LOG.error("readOrBuilt roots = " + StringUtil.join(stubRoots, stubToString, ", ") + "; " + StubTreeLoader.getFileViewProviderMismatchDiagnostics(viewProvider));
                                            this.rebuildStub();
                                            return stubHolder;
                                        } else {
                                            int matchingRoot = 0;

                                            PsiFileImpl eachPsiRoot;
                                            for(Iterator var12 = roots.iterator(); var12.hasNext(); eachPsiRoot.putUserData(ObjectStubTree.LAST_STUB_TREE_HASH, null)) {
                                                Pair<IStubFileElementType, PsiFile> root = (Pair)var12.next();
                                                PsiFileStub matchingStub = stubRoots[matchingRoot++];
                                                eachPsiRoot = (PsiFileImpl)root.second;
                                                ((StubBase)matchingStub).setPsi(eachPsiRoot);
                                                StubTree stubTree = new StubTree(matchingStub);
                                                FileElement fileElement = eachPsiRoot.getTreeElement();
                                                if (fileElement != null) {
                                                    stubTree.setDebugInfo("created in getStubTree(), with AST");
                                                    TreeUtil.bindStubsToTree(eachPsiRoot, stubTree, fileElement);
                                                } else {
                                                    stubTree.setDebugInfo("created in getStubTree(), no AST");
                                                    if (eachPsiRoot == this) {
                                                        stubHolder = stubTree;
                                                    }

                                                    eachPsiRoot.myStub = new SoftReference(stubTree);
                                                }
                                            }

                                            assert this.derefStub() == stubHolder : "Current file not in root list: " + roots + ", vp=" + viewProvider;

                                            return stubHolder;
                                        }
                                    }
                                }
                            }
                        }
                    }
*/
                }
            }
        }
    }

    @Nullable
    private StubTree derefStub() {
        return (StubTree)SoftReference.dereference(this.myStub);
    }

    protected PsiFileImpl cloneImpl(FileElement treeElementClone) {
        PsiFileImpl clone = (PsiFileImpl)super.clone();
        clone.setTreeElementPointer(treeElementClone);
        treeElementClone.setPsi(clone);
        return clone;
    }

    private boolean isKeepTreeElementByHardReference() {
        return !this.getViewProvider().isEventSystemEnabled();
    }

    @NotNull
    private Getter<FileElement> createTreeElementPointer(@NotNull FileElement treeElement) {
        return (Getter)(this.isKeepTreeElementByHardReference() ? treeElement : (Getter)(true ? new PatchedWeakReference(treeElement) : new SoftReference(treeElement)));
    }

    public PsiElement getNavigationElement() {
        return this;
    }

    public PsiElement getOriginalElement() {
        return this;
    }

    @NotNull
    public final FileElement calcTreeElement() {
        FileElement treeElement = this.getTreeElement();
        return treeElement != null ? treeElement : this.loadTreeElement();
    }

    @NotNull
    public PsiElement[] getChildren() {
        return this.calcTreeElement().getChildrenAsPsiElements((TokenSet)null, PsiElement.ARRAY_FACTORY);
    }

    public PsiElement getFirstChild() {
        return SharedImplUtil.getFirstChild(this.getNode());
    }

    public PsiElement getLastChild() {
        return SharedImplUtil.getLastChild(this.getNode());
    }

    public void acceptChildren(@NotNull PsiElementVisitor visitor) {
        SharedImplUtil.acceptChildren(visitor, this.getNode());
    }

    public int getStartOffsetInParent() {
        return this.calcTreeElement().getStartOffsetInParent();
    }

    public int getTextOffset() {
        return this.calcTreeElement().getTextOffset();
    }

    public boolean textMatches(@NotNull CharSequence text) {
        return this.calcTreeElement().textMatches(text);
    }

    public boolean textMatches(@NotNull PsiElement element) {
        return this.calcTreeElement().textMatches(element);
    }

    public boolean textContains(char c) {
        return this.calcTreeElement().textContains(c);
    }

    public final PsiElement copy() {
        return this.clone();
    }

    public PsiElement add(@NotNull PsiElement element) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        TreeElement elementCopy = ChangeUtil.copyToElement(element);
        this.calcTreeElement().addInternal(elementCopy, elementCopy, (ASTNode)null, (Boolean)null);
        elementCopy = ChangeUtil.decodeInformation(elementCopy);
        return SourceTreeToPsiMap.treeElementToPsi(elementCopy);
    }

    public PsiElement addBefore(@NotNull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        TreeElement elementCopy = ChangeUtil.copyToElement(element);
        this.calcTreeElement().addInternal(elementCopy, elementCopy, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.TRUE);
        elementCopy = ChangeUtil.decodeInformation(elementCopy);
        return SourceTreeToPsiMap.treeElementToPsi(elementCopy);
    }

    public PsiElement addAfter(@NotNull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        TreeElement elementCopy = ChangeUtil.copyToElement(element);
        this.calcTreeElement().addInternal(elementCopy, elementCopy, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.FALSE);
        elementCopy = ChangeUtil.decodeInformation(elementCopy);
        return SourceTreeToPsiMap.treeElementToPsi(elementCopy);
    }

    public final void checkAdd(@NotNull PsiElement element) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
    }

    public PsiElement addRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
        return SharedImplUtil.addRange(this, first, last, (ASTNode)null, (Boolean)null);
    }

    public PsiElement addRangeBefore(@NotNull PsiElement first, @NotNull PsiElement last, PsiElement anchor) throws IncorrectOperationException {
        return SharedImplUtil.addRange(this, first, last, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.TRUE);
    }

    public PsiElement addRangeAfter(PsiElement first, PsiElement last, PsiElement anchor) throws IncorrectOperationException {
        return SharedImplUtil.addRange(this, first, last, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.FALSE);
    }

    public void deleteChildRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        if (first == null) {
            LOG.assertTrue(last == null);
        } else {
            ASTNode firstElement = SourceTreeToPsiMap.psiElementToTree(first);
            ASTNode lastElement = SourceTreeToPsiMap.psiElementToTree(last);
            CompositeElement treeElement = this.calcTreeElement();
            LOG.assertTrue(firstElement.getTreeParent() == treeElement);
            LOG.assertTrue(lastElement.getTreeParent() == treeElement);
            CodeEditUtil.removeChildren(treeElement, firstElement, lastElement);
        }
    }

    public PsiElement replace(@NotNull PsiElement newElement) throws IncorrectOperationException {
        CompositeElement treeElement = this.calcTreeElement();
        return SharedImplUtil.doReplace(this, treeElement, newElement);
    }

    public PsiReference getReference() {
        return null;
    }

    @NotNull
    public PsiReference[] getReferences() {
        return SharedPsiElementImplUtil.getReferences(this);
    }

    public boolean processDeclarations(@NotNull PsiScopeProcessor processor, @NotNull ResolveState state, PsiElement lastParent, @NotNull PsiElement place) {
        return true;
    }

    @NotNull
    public GlobalSearchScope getResolveScope() {
        return ResolveScopeManager.getElementResolveScope(this);
    }

    @NotNull
    public SearchScope getUseScope() {
        return ResolveScopeManager.getElementUseScope(this);
    }

    public ItemPresentation getPresentation() {
        return new ItemPresentation() {
            public String getPresentableText() {
                return PsiFileImpl.this.getName();
            }

            public String getLocationString() {
                PsiDirectory psiDirectory = PsiFileImpl.this.getParent();
                return psiDirectory != null ? psiDirectory.getVirtualFile().getPresentableUrl() : null;
            }

            /*public Icon getIcon(boolean open) {
                return PsiFileImpl.this.getIcon(0);
            }*/
        };
    }

    public void navigate(boolean requestFocus) {
        assert this.canNavigate() : this;

        PsiNavigationSupport.getInstance().getDescriptor(this).navigate(requestFocus);
    }

    public boolean canNavigate() {
        return PsiNavigationSupport.getInstance().canNavigate(this);
    }

    public boolean canNavigateToSource() {
        return this.canNavigate();
    }

    @NotNull
    public FileASTNode getNode() {
        return this.calcTreeElement();
    }

    public boolean isEquivalentTo(PsiElement another) {
        return this == another;
    }

    @NotNull
    public StubTree calcStubTree() {
        FileElement fileElement = this.calcTreeElement();
        StubTree tree = (StubTree)SoftReference.dereference((Reference)fileElement.getUserData(STUB_TREE_IN_PARSED_TREE));
        if (tree != null) {
            return tree;
        } else {
            StubTree var10000;
            synchronized(this.myStubFromTreeLock) {
                tree = (StubTree)SoftReference.dereference((Reference)fileElement.getUserData(STUB_TREE_IN_PARSED_TREE));
                if (tree == null) {
                    IStubFileElementType contentElementType = this.getElementTypeForStubBuilder();
                    if (contentElementType == null) {
                        VirtualFile vFile = this.getVirtualFile();
                        String message = "ContentElementType: " + this.getContentElementType() + "; file: " + this + "\n\t" + "Boolean.TRUE.equals(getUserData(BUILDING_STUB)) = " + Boolean.TRUE.equals(this.getUserData(BUILDING_STUB)) + "\n\t" + "getTreeElement() = " + this.getTreeElement() + "\n\t" + "vFile instanceof VirtualFileWithId = " + (vFile instanceof VirtualFileWithId) + "\n\t" + "StubUpdatingIndex.canHaveStub(vFile) = " + StubTreeLoader.getInstance().canHaveStub(vFile);
                        this.rebuildStub();
                        throw new AssertionError(message);
                    }

                    StubElement currentStubTree = contentElementType.getBuilder().buildStubTree(this);
                    if (currentStubTree == null) {
                        throw new AssertionError("Stub tree wasn't built for " + contentElementType + "; file: " + this);
                    }

                    tree = new StubTree((PsiFileStub)currentStubTree);
                    tree.setDebugInfo("created in calcStubTree");

                    try {
                        TreeUtil.bindStubsToTree(this, tree, fileElement);
                    } catch (TreeUtil.StubBindingException var8) {
                        TreeUtil.StubBindingException e = var8;
                        this.rebuildStub();
                        throw new RuntimeException("Stub and PSI element type mismatch in " + this.getName(), e);
                    }

                    fileElement.putUserData(STUB_TREE_IN_PARSED_TREE, new SoftReference(tree));
                }

                var10000 = tree;
            }

            return var10000;
        }
    }

    @Nullable
    public StubBasedPsiElementBase<?> obtainPsi(@NotNull AstPath path, @NotNull Factory<StubBasedPsiElementBase<?>> creator) {
       /* if (useStrongRefs()) {
            return null;
        }

        StubBasedPsiElementBase<?> psi = myRefToPsi.getCachedPsi(path);
        if (psi != null) return psi;

        synchronized (PsiLock.LOCK) {
            psi = myRefToPsi.getCachedPsi(path);
            return psi != null ? psi : myRefToPsi.cachePsi(path, creator.create());
        }*/
        return null;
    }

    private void rebuildStub() {

    }

    public static void putInfo(PsiFile psiFile, Map<String, String> info) {
        info.put("fileName", psiFile.getName());
        info.put("fileType", psiFile.getFileType().toString());
    }

    public String toString() {
        return this.myElementType.toString();
    }

    public boolean mayCacheAst() {
        return false;
    }

    public boolean useStrongRefs() {
        return false;//myUseStrongRefs;
    }
}