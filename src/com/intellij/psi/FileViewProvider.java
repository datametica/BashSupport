/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.psi;

import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Manages language-specific access to PSI for a single file.
 * <p/>
 * Custom providers are registered via { FileViewProviderFactory}.
 * <p/>
 * Please see <a href="http://confluence.jetbrains.net/display/IDEADEV/IntelliJ+IDEA+Architectural+Overview">IntelliJ IDEA Architectural Overview </a>
 * for high-level overview.
 *
 *  PsiFile#getViewProvider()
 *  PsiManager#findViewProvider(VirtualFile)
 */
public interface FileViewProvider extends Cloneable, UserDataHolder {



  /**
   * @return the document corresponding to this file. Can be null for binary or files without events enabled.
   * 
   *  FileType#isBinary()
   *  PsiBinaryFile
   *  #isEventSystemEnabled()
   */
  @Nullable
  Document getDocument();

  /**
   * @return the contents of this file view provider, which are parsed into PSI trees. May or may not be equal 
   * to the contents of the corresponding document. The latter happens for non-committed documents.
   * If the document is modified but not yet committed, the result is equivalent to { PsiDocumentManager#getLastCommittedText(Document)}.
   * 
   *  #getDocument()
   *  PsiDocumentManager#isUncommited(Document)
   */
  @NotNull
  CharSequence getContents();

  /**
   * @return the virtual file corresponding to this view provider. Physical or an instance of { LightVirtualFile} for most non-physical files.
   */
  @NotNull
  VirtualFile getVirtualFile();

  /**
   * @return the language of the main PSI tree (or the only one in a single-tree view providers). Used when returning a PsiFile from 
   * { PsiManager#findFile(VirtualFile)},
   * { PsiDocumentManager#getPsiFile(Document)} etc.
   */
  @NotNull
  Language getBaseLanguage();

  /**
   * @return all languages this file supports, in no particular order.
   * 
   *  #getPsi(com.intellij.lang.Language)
   */
  @NotNull
  Set<Language> getLanguages();

  /**
   * @param target target language
   * @return PsiFile for given language, or <code>null</code> if the language not present
   */
  PsiFile getPsi(@NotNull Language target);

  /**
   * @return all PSI files for this view provider. In most cases, just one main file. For multi-root languages, several files. The files' languages
   * should be the same as { #getLanguages()}. The main file which corresponds to { #getBaseLanguage()}, should be the first one. Otherwise
   * the order is non-deterministic and should not be relied upon.
   */
  @NotNull
  List<PsiFile> getAllFiles();

  /**
   * @return whether PSI events are fired when changes occur inside PSI in this view provider. True for physical files and for some non-physical as well.
   * 
   *  PsiTreeChangeListener
   *  PsiFileFactory#createFileFromText(String, FileType, CharSequence, long, boolean)
   *  PsiFile#isPhysical()
   */
  boolean isEventSystemEnabled();

  /**
   * @return whether this file corresponds to a file on a disk. For such files, { PsiFile#getVirtualFile()} returns non-null.
   * Not to be confused with { PsiFile#isPhysical()} which (for historical reasons) returns {@code getViewProvider().isEventSystemEnabled()}
   * 
   *  #isEventSystemEnabled()
   *  PsiFile#isPhysical()
   */
  boolean isPhysical();

  /**
   * @return a number to quickly check if contents of this view provider have diverged from the corresponding { VirtualFile} or { Document}.
   * If a document is modified but not yet committed, the result is the same as { PsiDocumentManager#getLastCommittedStamp(Document)}
   * 
   *  VirtualFile#getModificationStamp()
   *  Document#getModificationStamp()
   */
  long getModificationStamp();

  /**
   * @param rootLanguage one of the root languages
   * @return whether the PSI file with the specified root language supports incremental reparse.
   * 
   *  #getLanguages()
   */
  boolean supportsIncrementalReparse(@NotNull Language rootLanguage);

  /**
   * Invoked when any PSI change happens in any of the PSI files corresponding to this view provider.
   * @param psiFile the file where PSI has just been changed.
   */
  void rootChanged(@NotNull PsiFile psiFile);

  /**
   * Invoked before document or VFS changes are processed that affect PSI inside the corresponding file.
   */
  void beforeContentsSynchronized();

  /**
   * Invoked after PSI in the corresponding file is synchronized with the corresponding document, which can happen
   * after VFS, document or PSI changes.<p/>
   * 
   * Multi-language file view providers may override this method to recalculate template data languages.
   * 
   *  #getLanguages()
   */
  void contentsSynchronized();

  /**
   * @return a copy of this view provider, built on a { LightVirtualFile}, not physical and with PSI events disabled.
   * 
   *  #isPhysical()
   *  #isEventSystemEnabled()
   *  #createCopy(VirtualFile)
   */
  FileViewProvider clone();

  /**
   * @param offset an offset in the file
   * @return the deepest (leaf) PSI element in the main PSI tree at the specified offset.
   * 
   *  #getBaseLanguage()
   *  #findElementAt(int, Class)
   *  #findElementAt(int, com.intellij.lang.Language)
   *  PsiFile#findElementAt(int)
   */
  @Nullable
  PsiElement findElementAt(int offset);

  /**
   * @param offset an offset in the file
   * @return a reference in the main PSI tree at the specified offset.
   * 
   *  #getBaseLanguage()
   *  PsiFile#findReferenceAt(int)
   *  #findReferenceAt(int, com.intellij.lang.Language)
   */
  @Nullable
  PsiReference findReferenceAt(int offset);

  /**
   * @param offset an offset in the file
   * @return the deepest (leaf) PSI element in the PSI tree with the specified root language at the specified offset.
   *
   *  #getBaseLanguage()
   *  #findElementAt(int)
   */
  @Nullable
  PsiElement findElementAt(int offset, @NotNull Language language);

  /**
   * @param offset an offset in the file
   * @return the deepest (leaf) PSI element in the PSI tree with the specified root language class at the specified offset.
   *
   *  #getBaseLanguage()
   *  #findElementAt(int)
   */
  @Nullable
  PsiElement findElementAt(int offset, @NotNull Class<? extends Language> lang);

  /**
   * @param offsetInElement an offset in the file
   * @return a reference in the PSI tree with the specified root language at the specified offset.
   *
   *  #getBaseLanguage()
   *  PsiFile#findReferenceAt(int)
   *  #findReferenceAt(int)
   */
  @Nullable
  PsiReference findReferenceAt(int offsetInElement, @NotNull Language language);

  /**
   * Creates a copy of this view provider linked with the give (typically light) file.
   * The result provider is required to be NOT event-system-enabled.
   * 
   *  LightVirtualFile
   *  #isEventSystemEnabled()
   */
  @NotNull
  FileViewProvider createCopy(@NotNull VirtualFile copy);

  /**
   * @return the PSI root for which stubs are to be built if supported. By default it's the main root.
   * 
   *  #getBaseLanguage()
   */
  @NotNull
  PsiFile getStubBindingRoot();

  /**
   * @return the same as {@code getVirtualFile().getFileType()}, but cached.
   */
  @NotNull
  FileType getFileType();
}
