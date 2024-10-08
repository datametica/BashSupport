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
package com.intellij.openapi.vfs;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * Represents a virtual file system.
 *
 *  VirtualFile
 *  VirtualFileManager
 */
public abstract class VirtualFileSystem {
  protected VirtualFileSystem() { }

  /**
   * Gets the protocol for this file system. Protocols should differ for all file systems.
   * Should be the same as corresponding { KeyedLazyInstanceEP#key}.
   *
   * @return String representing the protocol
   *  VirtualFile#getUrl
   *  VirtualFileManager#getFileSystem
   */
  @NonNls
  @NotNull
  public abstract String getProtocol();

  /**
   * Searches for the file specified by given path. Path is a string which uniquely identifies file within given
   * { VirtualFileSystem}. Format of the path depends on the concrete file system.
   * For {@code LocalFileSystem} it is an absolute path (both Unix- and Windows-style separator chars are allowed).
   *
   * @param path the path to find file by
   * @return a virtual file if found, {@code null} otherwise
   */
  @Nullable
  public abstract VirtualFile findFileByPath(@NotNull @NonNls String path);

  /**
   * Fetches presentable URL of file with the given path in this file system.
   *
   * @param path the path to get presentable URL for
   * @return presentable URL
   *  VirtualFile#getPresentableUrl
   */
  @NotNull
  public String extractPresentableUrl(@NotNull String path) {
    return path.replace('/', File.separatorChar);
  }

  /**
   * Refreshes the cached information for all files in this file system from the physical file system.<p>
   * <p/>
   * If {@code asynchronous} is {@code false} this method should be only called within write-action.
   * See { Application#runWriteAction}.
   *
   * @param asynchronous if {@code true} then the operation will be performed in a separate thread,
   *                     otherwise will be performed immediately
   *  VirtualFile#refresh
   *  VirtualFileManager#syncRefresh
   *  VirtualFileManager#asyncRefresh
   */
  public abstract void refresh(boolean asynchronous);

  /**
   * Refreshes only the part of the file system needed for searching the file by the given path and finds file
   * by the given path.<br>
   * <p/>
   * This method is useful when the file was created externally and you need to find <code>{ VirtualFile}</code>
   * corresponding to it.<p>
   * <p/>
   * If this method is invoked not from Swing event dispatch thread, then it must not happen inside a read action. The reason is that
   * then the method call won't return until proper VFS events are fired, which happens on Swing thread and in write action. So invoking
   * this method in a read action would result in a deadlock.
   *
   * @param path the path
   * @return <code>{ VirtualFile}</code> if the file was found, {@code null} otherwise
   */
  @Nullable
  public abstract VirtualFile refreshAndFindFileByPath(@NotNull String path);

  /**
   * Implementation of deleting files in this file system
   *
   *  VirtualFile#delete(Object)
   */
  protected abstract void deleteFile(Object requestor, @NotNull VirtualFile vFile) throws IOException;

  /**
   * Implementation of moving files in this file system
   *
   *  VirtualFile#move(Object,VirtualFile)
   */
  protected abstract void moveFile(Object requestor, @NotNull VirtualFile vFile, @NotNull VirtualFile newParent) throws IOException;

  /**
   * Implementation of renaming files in this file system
   *
   *  VirtualFile#rename(Object,String)
   */
  protected abstract void renameFile(Object requestor, @NotNull VirtualFile vFile, @NotNull String newName) throws IOException;

  /**
   * Implementation of adding files in this file system
   *
   *  VirtualFile#createChildData(Object,String)
   */
  @NotNull
  protected abstract VirtualFile createChildFile(Object requestor, @NotNull VirtualFile vDir, @NotNull String fileName) throws IOException;

  /**
   * Implementation of adding directories in this file system
   *
   *  VirtualFile#createChildDirectory(Object,String)
   */
  @NotNull
  protected abstract VirtualFile createChildDirectory(Object requestor, @NotNull VirtualFile vDir, @NotNull String dirName) throws IOException;

  /**
   * Implementation of copying files in this file system
   *
   *  VirtualFile#copy(Object,VirtualFile,String)
   */
  @NotNull
  protected abstract VirtualFile copyFile(Object requestor,
                                          @NotNull VirtualFile virtualFile,
                                          @NotNull VirtualFile newParent,
                                          @NotNull String copyName) throws IOException;

  public abstract boolean isReadOnly();

  public boolean isCaseSensitive() {
    return true;
  }

  public boolean isValidName(@NotNull String name) {
    return !name.isEmpty() && name.indexOf('\\') < 0 && name.indexOf('/') < 0;
  }
}