/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.fileEditor;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.vfs.SavingRequestor;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tracks the correspondence between { VirtualFile} instances and corresponding { Document} instances.
 * Manages the saving of changes to disk.
 */
public abstract class FileDocumentManager implements SavingRequestor {
  @NotNull
  public static FileDocumentManager getInstance() {
    return null;
  }

  /**
   * Returns the document for the specified virtual file.<p/>
   * 
   * Documents are cached on weak or strong references, depending on the nature of the virtual file. If the document for the given virtual file is not yet cached,
   * the file's contents are read from VFS and loaded into heap memory. An appropriate encoding is used. All line separators are converted to <code>\n</code>.<p/>
   * 
   * Should be invoked in a read action.
   * 
   * @param file the file for which the document is requested.
   * @return the document, or null if the file represents a directory, or is binary without an associated decompiler,
   * or is too large.
   *  VirtualFile#contentsToByteArray()
   *  Application#runReadAction(Computable)
   */
  @Nullable
  public abstract Document getDocument(@NotNull VirtualFile file);

  /**
   * Returns the document for the specified file which has already been loaded into memory.<p/>
   * 
   * Client code shouldn't normally use this method, because it's unpredictable and any garbage collection can result in it returning null.
   *
   * @param file the file for which the document is requested.
   * @return the document, or null if the specified virtual file hasn't been loaded into memory.
   */
  @Nullable
  public abstract Document getCachedDocument(@NotNull VirtualFile file);

  /**
   * Returns the virtual file corresponding to the specified document.
   *
   * @param document the document for which the virtual file is requested.
   * @return the file, or null if the document wasn't created from a virtual file.
   */
  @Nullable
  public abstract VirtualFile getFile(@NotNull Document document);

  /**
   * Saves the document without stripping the trailing spaces or adding a blank line in the end of the file.<p/>
   * 
   * Should be invoked on the event dispatch thread.
   * 
   * @param document the document to save.
   */
  public abstract void saveDocumentAsIs(@NotNull Document document);

  /**
   * Checks if the document has unsaved changes.
   *
   * @param document the document to check.
   * @return true if the document has unsaved changes, false otherwise.
   */
  public abstract boolean isDocumentUnsaved(@NotNull Document document);

  /**
   * Discards unsaved changes for the specified document and reloads it from disk.
   *
   * @param document the document to reload.
   */
  public abstract void reloadFromDisk(@NotNull Document document);
}
