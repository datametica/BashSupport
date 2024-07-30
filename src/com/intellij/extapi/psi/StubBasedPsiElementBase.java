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

package com.intellij.extapi.psi;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiLock;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.impl.source.SubstrateRef;
import com.intellij.psi.impl.source.tree.FileElement;
import com.intellij.psi.impl.source.tree.SharedImplUtil;
import com.intellij.psi.stubs.*;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;



public class StubBasedPsiElementBase<T extends StubElement> extends ASTDelegatePsiElement {
  private static final Logger LOG = Logger.getInstance("#com.intellij.extapi.psi.StubBasedPsiElementBase");
  public static final boolean ourTraceStubAstBinding = "true".equals(System.getProperty("trace.stub.ast.binding", "false"));
  private volatile T myStub;
  private volatile ASTNode myNode;
  private final IElementType myElementType;
  private volatile SubstrateRef mySubstrateRef;

  public StubBasedPsiElementBase(@NotNull T stub, @NotNull IStubElementType nodeType) {
    this.myStub = stub;
    this.myElementType = nodeType;
    this.myNode = null;
    mySubstrateRef = new SubstrateRef.StubRef(stub);
  }

  public StubBasedPsiElementBase(@NotNull ASTNode node) {
    this.myNode = node;
    this.myElementType = node.getElementType();
    mySubstrateRef = SubstrateRef.createAstStrongRef(node);
  }

  @NotNull
  public ASTNode getNode() {
    ASTNode node = this.myNode;
    if (node == null) {
      PsiFileImpl file = (PsiFileImpl)this.getContainingFile();
      if (!file.isValid()) {
        throw new PsiInvalidElementAccessException(this);
      }

      FileElement treeElement = file.getTreeElement();
      if (treeElement != null && this.myNode == null) {
        return this.notBoundInExistingAst(file, treeElement);
      }

      treeElement = file.calcTreeElement();
      node = this.myNode;
      if (node == null) {
        return this.failedToBindStubToAst(file, treeElement);
      }
    }

    return node;
  }

  public final void setStubIndex(int stubIndex) {
    //myStubIndex = stubIndex;
  }

  private ASTNode failedToBindStubToAst(PsiFileImpl file, FileElement fileElement) {
    VirtualFile vFile = file.getVirtualFile();
    StubTree stubTree = file.getStubTree();
    String stubString = stubTree != null ? ((PsiFileStubImpl)stubTree.getRoot()).printTree() : "is null";
    String astString = DebugUtil.treeToString(fileElement, true);
    if (!ourTraceStubAstBinding) {
      stubString = StringUtil.trimLog(stubString, 1024);
      astString = StringUtil.trimLog(astString, 1024);
    }

    String message = "Failed to bind stub to AST for element " + this.getClass() + " in " + (vFile == null ? "<unknown file>" : vFile.getPath()) + "\nFile:\n" + file + "@" + System.identityHashCode(file) + "\nFile stub tree:\n" + stubString + "\nLoaded file AST:\n" + astString;
    if (ourTraceStubAstBinding) {
      message = message + this.dumpCreationTraces(fileElement);
    }

    throw new IllegalArgumentException(message);
  }

  private String dumpCreationTraces(FileElement fileElement) {
/*
    final StringBuilder traces = new StringBuilder("\nNow " + Thread.currentThread() + "\n");
    traces.append("My creation trace:\n").append((String)this.getUserData(CREATION_TRACE));
    traces.append("AST creation traces:\n");
    fileElement.acceptTree(new RecursiveTreeElementWalkingVisitor(false) {
      public void visitComposite(CompositeElement composite) {
        PsiElement psi = composite.getPsi();
        if (psi != null) {
          traces.append(psi).append("@").append(System.identityHashCode(psi)).append("\n");
          String trace = (String)psi.getUserData(StubBasedPsiElementBase.CREATION_TRACE);
          if (trace != null) {
            traces.append(trace).append("\n");
          }
        }

        super.visitComposite(composite);
      }
    });
    return traces.toString();
*/
    return null;
  }

  @NotNull
  public final SubstrateRef getSubstrateRef() {
    return mySubstrateRef;
  }

  private ASTNode notBoundInExistingAst(PsiFileImpl file, FileElement treeElement) {
    String message = "file=" + file + "; tree=" + treeElement;

    for(PsiElement each = this; each != null; each = ((StubBasedPsiElementBase)each).getParentByStub()) {
      message = message + "\n each of class " + each.getClass() + "; valid=" + ((PsiElement)each).isValid();
      if (!(each instanceof StubBasedPsiElementBase)) {
        if (each instanceof PsiFile) {
          message = message + "; same file=" + (each == file) + "; current tree= " + file.getTreeElement() + "; stubTree=" + file.getStubTree() + "; physical=" + file.isPhysical();
        }
        break;
      }

      message = message + "; node=" + ((StubBasedPsiElementBase)each).myNode + "; stub=" + ((StubBasedPsiElementBase)each).myStub;
    }

    for(StubElement eachStub = this.myStub; eachStub != null; eachStub = eachStub.getParentStub()) {
      message = message + "\n each stub " + (eachStub instanceof PsiFileStubImpl ? ((PsiFileStubImpl)eachStub).getDiagnostics() : eachStub);
    }

    if (ourTraceStubAstBinding) {
      message = message + this.dumpCreationTraces(treeElement);
    }

    throw new AssertionError(message);
  }

  public final void setNode(@NotNull ASTNode node) {
    this.myNode = node;
  }

  @NotNull
  public Language getLanguage() {
    return this.myElementType.getLanguage();
  }

  @NotNull
  public PsiFile getContainingFile() {
    StubElement stub = this.myStub;
    PsiFile psi;
    if (stub != null) {
      while(true) {
        if (stub instanceof PsiFileStub) {
          psi = (PsiFile)stub.getPsi();
          if (psi != null) {
            return psi;
          }
          synchronized(PsiLock.LOCK) {
            if (this.myStub != null) {
              String reason = ((PsiFileStubImpl)stub).getInvalidationReason();
              PsiInvalidElementAccessException exception = new PsiInvalidElementAccessException(this, "no psi for file stub " + stub + ", invalidation reason=" + reason, (Throwable)null);
              if ("stub-psi mismatch".equals(reason)) {
                throw new ProcessCanceledException(exception);
              }

              throw exception;
            }
            break;
          }
        }

        stub = stub.getParentStub();
      }
    }

    psi = super.getContainingFile();
    return psi;
  }

  public boolean isWritable() {
    return this.getContainingFile().isWritable();
  }

  public boolean isValid() {
    T stub = this.myStub;
    if (stub != null) {
      StubElement parent = stub.getParentStub();
      if (parent == null) {
        LOG.error("No parent for stub " + stub + " of class " + stub.getClass());
        return false;
      } else {
        PsiElement psi = parent.getPsi();
        return psi != null && psi.isValid();
      }
    } else {
      return super.isValid();
    }
  }

  public boolean isPhysical() {
    return this.getContainingFile().isPhysical();
  }

  public PsiElement getContext() {
    T stub = this.myStub;
    return stub != null && !(stub instanceof PsiFileStub) ? stub.getParentStub().getPsi() : super.getContext();
  }

  protected final PsiElement getParentByStub() {
    StubElement<?> stub = this.getStub();
    return stub != null ? stub.getParentStub().getPsi() : SharedImplUtil.getParent(this.getNode());
  }

  public void subtreeChanged() {
    super.subtreeChanged();
    this.setStub((T) null);
  }

  protected final PsiElement getParentByTree() {
    return SharedImplUtil.getParent(this.getNode());
  }

  public PsiElement getParent() {
    return this.getParentByTree();
  }

  @NotNull
  public IStubElementType getElementType() {
    if (!(this.myElementType instanceof IStubElementType)) {
      throw new AssertionError("Not a stub type: " + this.myElementType + " in " + this.getClass());
    } else {
      return (IStubElementType)this.myElementType;
    }
  }

  @Nullable
  public T getStub() {
    ProgressIndicatorProvider.checkCanceled();
    return this.myStub;
  }

  public final void setStub(@Nullable T stub) {
    this.myStub = stub;
  }

  /** @deprecated */
  @Deprecated
  @Nullable
  protected PsiElement getStubOrPsiParent() {
    T stub = this.myStub;
    return stub != null ? stub.getParentStub().getPsi() : this.getParent();
  }

  protected Object clone() {
    StubBasedPsiElementBase copy = (StubBasedPsiElementBase)super.clone();
    copy.myStub = null;
    return copy;
  }
}
