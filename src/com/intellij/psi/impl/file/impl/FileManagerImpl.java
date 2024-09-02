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

package com.intellij.psi.impl.file.impl;

import com.intellij.injected.editor.DocumentWindow;
import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.LowMemoryWatcher;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.psi.impl.PsiTreeChangeEventImpl;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

public class FileManagerImpl implements FileManager {
  private static final Logger LOG = Logger.getInstance("#com.intellij.psi.impl.file.impl.FileManagerImpl");
  private final Key<FileViewProvider> myPsiHardRefKey = Key.create("HARD_REFERENCE_TO_PSI"); //non-static!

  private final FileIndexFacade myFileIndex;

  private final ConcurrentMap<VirtualFile, PsiDirectory> myVFileToPsiDirMap = ContainerUtil.createConcurrentSoftValueMap();
  private final ConcurrentMap<VirtualFile, FileViewProvider> myVFileToViewProviderMap = ContainerUtil.createConcurrentWeakValueMap();

  private boolean myInitialized;
  private boolean myDisposed;

  private final FileDocumentManager myFileDocumentManager;
  private final MessageBusConnection myConnection;

  public FileManagerImpl(FileDocumentManager fileDocumentManager, FileIndexFacade fileIndex) {
    myFileIndex = fileIndex;
    myConnection = null;

    myFileDocumentManager = fileDocumentManager;

    LowMemoryWatcher.register(new Runnable() {
      @Override
      public void run() {
        processQueue();
      }
    }, this);
  }

  private static final VirtualFile NULL = new LightVirtualFile();

  public void processQueue() {
    // just to call processQueue()
    myVFileToViewProviderMap.remove(NULL);
  }

  public static void clearPsiCaches(@NotNull FileViewProvider provider) {
    if (provider instanceof SingleRootFileViewProvider) {
      for (PsiFile root : ((SingleRootFileViewProvider)provider).getCachedPsiFiles()) {
        if (root instanceof PsiFileImpl) {
          ((PsiFileImpl)root).clearCaches();
        }
      }
    } else {
      for (Language language : provider.getLanguages()) {
        final PsiFile psi = provider.getPsi(language);
        if (psi instanceof PsiFileImpl) {
          ((PsiFileImpl)psi).clearCaches();
        }
      }
    }
  }

  public void forceReload(@NotNull VirtualFile vFile) {
    LanguageSubstitutors.cancelReparsing(vFile);
    FileViewProvider viewProvider = findCachedViewProvider(vFile);
    if (viewProvider == null) {
      return;
    }

    setViewProvider(vFile, null);

    VirtualFile dir = vFile.getParent();
    PsiDirectory parentDir = dir == null ? null : getCachedDirectory(dir);
    PsiTreeChangeEventImpl event = new PsiTreeChangeEventImpl();
    if (parentDir != null) {
      event.setParent(parentDir);
      //myManager.childrenChanged(event);
    }
    else {
      firePropertyChangedForUnloadedPsi(event, vFile);
    }
  }

  void firePropertyChangedForUnloadedPsi(@NotNull PsiTreeChangeEventImpl event, @NotNull VirtualFile vFile) {
    event.setPropertyName(PsiTreeChangeEvent.PROP_UNLOADED_PSI);
    event.setOldValue(vFile);
    event.setNewValue(vFile);
  }

  @Override
  public void dispose() {
    if (myInitialized) {
      myConnection.disconnect();
    }
    clearViewProviders();

    myDisposed = true;
  }

  private void clearViewProviders() {
    DebugUtil.startPsiModification("clearViewProviders");
    try {
      for (final FileViewProvider provider : myVFileToViewProviderMap.values()) {
        markInvalidated(provider);
      }
      myVFileToViewProviderMap.clear();
    }
    finally {
      DebugUtil.finishPsiModification();
    }
  }

  @Override
  @TestOnly
  public void cleanupForNextTest() {

    myVFileToPsiDirMap.clear();
    //((PsiModificationTrackerImpl)myManager.getModificationTracker()).incCounter();
  }

  @Override
  @NotNull
  public FileViewProvider findViewProvider(@NotNull final VirtualFile file) {
    assert !file.isDirectory();
    FileViewProvider viewProvider = findCachedViewProvider(file);
    if (viewProvider != null) return viewProvider;

    viewProvider = createFileViewProvider(file, true);
    if (file instanceof LightVirtualFile) {
      return file.putUserDataIfAbsent(myPsiHardRefKey, viewProvider);
    }
    return ConcurrencyUtil.cacheOrGet(myVFileToViewProviderMap, file, viewProvider);
  }

  @Override
  public FileViewProvider findCachedViewProvider(@NotNull final VirtualFile file) {
    FileViewProvider viewProvider = getFromInjected(file);
    if (viewProvider == null) viewProvider = myVFileToViewProviderMap.get(file);
    if (viewProvider == null) viewProvider = file.getUserData(myPsiHardRefKey);
    return viewProvider;
  }

  @Nullable
  private FileViewProvider getFromInjected(@NotNull VirtualFile file) {
    if (file instanceof VirtualFileWindow) {
      DocumentWindow document = ((VirtualFileWindow)file).getDocumentWindow();
      PsiFile psiFile = PsiDocumentManager.getInstance().getCachedPsiFile(document);
      if (psiFile == null) return null;
      return psiFile.getViewProvider();
    }
    return null;
  }

  @Override
  public void setViewProvider(@NotNull final VirtualFile virtualFile, @Nullable final FileViewProvider fileViewProvider) {
    FileViewProvider prev = findCachedViewProvider(virtualFile);
    if (prev == fileViewProvider) return;
    if (prev != null) {
      DebugUtil.startPsiModification(null);
      try {
        markInvalidated(prev);
        DebugUtil.onInvalidated(prev);
      }
      finally {
        DebugUtil.finishPsiModification();
      }
    }

    if (!(virtualFile instanceof VirtualFileWindow)) {
      if (fileViewProvider == null) {
        myVFileToViewProviderMap.remove(virtualFile);
      }
      else {
        if (virtualFile instanceof LightVirtualFile) {
          virtualFile.putUserData(myPsiHardRefKey, fileViewProvider);
        } else {
          myVFileToViewProviderMap.put(virtualFile, fileViewProvider);
        }
      }
    }
  }

  @Override
  @NotNull
  public FileViewProvider createFileViewProvider(@NotNull final VirtualFile file, boolean eventSystemEnabled) {
    FileType fileType = file.getFileType();
    Language language = LanguageUtil.getLanguageForPsi( file);
    FileViewProviderFactory factory = language == null
                                      ? FileTypeFileViewProviders.INSTANCE.forFileType(fileType)
                                      : LanguageFileViewProviders.INSTANCE.forLanguage(language);
    FileViewProvider viewProvider = factory == null ? null : factory.createFileViewProvider(file, language, eventSystemEnabled);

    return viewProvider == null ? new SingleRootFileViewProvider(file, eventSystemEnabled, fileType) : viewProvider;
  }

  void dispatchPendingEvents() {
    if (!myInitialized) {
      //LOG.error("Project is not yet initialized: "+myManager.getProject());
    }
    if (myDisposed) {
      //LOG.error("Project is already disposed: "+myManager.getProject());
    }

    myConnection.deliverImmediately();
  }

  @Override
  @Nullable
  public PsiFile findFile(@NotNull VirtualFile vFile) {
    if (vFile.isDirectory()) return null;

    if (!vFile.isValid()) {
      LOG.error("Invalid file: " + vFile);
      return null;
    }

    dispatchPendingEvents();
    final FileViewProvider viewProvider = findViewProvider(vFile);
    return viewProvider.getPsi(viewProvider.getBaseLanguage());
  }

  @Override
  @Nullable
  public PsiFile getCachedPsiFile(@NotNull VirtualFile vFile) {
    LOG.assertTrue(vFile.isValid(), "Invalid file");
    if (myDisposed) {
      //LOG.error("Project is already disposed: " + myManager.getProject());
    }
    if (!myInitialized) return null;

    dispatchPendingEvents();

    return getCachedPsiFileInner(vFile);
  }

  @Override
  @Nullable
  public PsiDirectory findDirectory(@NotNull VirtualFile vFile) {
    LOG.assertTrue(myInitialized, "Access to psi files should be performed only after startup activity");
    if (myDisposed) {
      //LOG.error("Access to psi files should not be performed after project disposal: "+myManager.getProject());
    }

    if (!vFile.isValid()) {
      LOG.error("File is not valid:" + vFile);
      return null;
    }

    if (!vFile.isDirectory()) return null;
    dispatchPendingEvents();

    return findDirectoryImpl(vFile);
  }

  @Nullable
  private PsiDirectory findDirectoryImpl(@NotNull VirtualFile vFile) {
    PsiDirectory psiDir = myVFileToPsiDirMap.get(vFile);
    if (psiDir != null) return psiDir;

    if (Registry.is("ide.hide.excluded.files".toString())) {
      if (myFileIndex.isExcludedFile(vFile)) return null;
    }
    else {
      if (myFileIndex.isUnderIgnored(vFile)) return null;
    }

    VirtualFile parent = vFile.getParent();
    if (parent != null) { //?
      findDirectoryImpl(parent);// need to cache parent directory - used for firing events
    }

    psiDir = PsiDirectoryFactory.getInstance().createDirectory(vFile);
    return ConcurrencyUtil.cacheOrGet(myVFileToPsiDirMap, vFile, psiDir);
  }

  public PsiDirectory getCachedDirectory(@NotNull VirtualFile vFile) {
    return myVFileToPsiDirMap.get(vFile);
  }

  private void markInvalidated(@NotNull FileViewProvider viewProvider) {
    if (viewProvider instanceof SingleRootFileViewProvider) {
      ((SingleRootFileViewProvider)viewProvider).markInvalidated();
    }
    VirtualFile virtualFile = viewProvider.getVirtualFile();
    Document document = FileDocumentManager.getInstance().getCachedDocument(virtualFile);
    if (document != null) {
      //((PsiDocumentManagerBase)PsiDocumentManager.getInstance(myManager.getProject())).associatePsi(document, null);
    }
    virtualFile.putUserData(myPsiHardRefKey, null);
  }

  @Nullable
  PsiFile getCachedPsiFileInner(@NotNull VirtualFile file) {
    FileViewProvider fileViewProvider = myVFileToViewProviderMap.get(file);
    if (fileViewProvider == null) fileViewProvider = file.getUserData(myPsiHardRefKey);
    return fileViewProvider instanceof SingleRootFileViewProvider
           ? ((SingleRootFileViewProvider)fileViewProvider).getCachedPsi(fileViewProvider.getBaseLanguage()) : null;
  }

  @NotNull
  @Override
  public List<PsiFile> getAllCachedFiles() {
    List<PsiFile> files = new ArrayList<PsiFile>();
    for (FileViewProvider provider : myVFileToViewProviderMap.values()) {
      if (provider instanceof SingleRootFileViewProvider) {
        ContainerUtil.addIfNotNull(files, ((SingleRootFileViewProvider)provider).getCachedPsi(provider.getBaseLanguage()));
      }
    }
    return files;
  }

  @Override
  public void reloadFromDisk(@NotNull PsiFile file) {
    reloadFromDisk(file, false);
  }

  void reloadFromDisk(@NotNull PsiFile file, boolean ignoreDocument) {
    VirtualFile vFile = file.getVirtualFile();
    assert vFile != null;

    if (file instanceof PsiBinaryFile) return;
    FileDocumentManager fileDocumentManager = myFileDocumentManager;
    Document document = fileDocumentManager.getCachedDocument(vFile);
    if (document != null && !ignoreDocument){
      fileDocumentManager.reloadFromDisk(document);
    }
    else {
      FileViewProvider latestProvider = createFileViewProvider(vFile, false);
      if (latestProvider.getPsi(latestProvider.getBaseLanguage()) instanceof PsiBinaryFile) {
        forceReload(vFile);
        return;
      }

      FileViewProvider viewProvider = file.getViewProvider();
      if (viewProvider instanceof SingleRootFileViewProvider) {
        ((SingleRootFileViewProvider)viewProvider).onContentReload();
      } else {
        LOG.error("Invalid view provider: " + viewProvider + " of " + viewProvider.getClass());
      }
    }
  }
}
