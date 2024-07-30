/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.util.io.URLUtil;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages virtual file systems.
 *
 *  VirtualFileSystem
 */
public abstract class VirtualFileManager implements ModificationTracker {
  public static final Topic<BulkFileListener> VFS_CHANGES =
    new Topic<BulkFileListener>("NewVirtualFileSystem changes", BulkFileListener.class);

  /**
   * Gets the instance of <code>VirtualFileManager</code>.
   *
   * @return <code>VirtualFileManager</code>
   */
  @NotNull
  public static VirtualFileManager getInstance() {
    return null;
  }

  /**
   * Gets VirtualFileSystem with the specified protocol.
   *
   * @param protocol String representing the protocol
   * @return { VirtualFileSystem}
   *  VirtualFileSystem#getProtocol
   */
  public abstract VirtualFileSystem getFileSystem(String protocol);

  /**
   * Searches for the file specified by given URL. URL is a string which uniquely identifies file in all
   * file systems.
   *
   * @param url the URL to find file by
   * @return <code>{ VirtualFile}</code> if the file was found, <code>null</code> otherwise
   *  VirtualFile#getUrl
   *  VirtualFileSystem#findFileByPath
   *  #refreshAndFindFileByUrl
   */
  @Nullable
  public abstract VirtualFile findFileByUrl(@NonNls @NotNull String url);

  /**
   * Constructs URL by specified protocol and path. URL is a string which uniquely identifies file in all
   * file systems.
   *
   * @param protocol the protocol
   * @param path     the path
   * @return URL
   */
  @NotNull
  public static String constructUrl(@NotNull String protocol, @NotNull String path) {
    return protocol + URLUtil.SCHEME_SEPARATOR + path;
  }

  /**
   * Extracts path from the given URL. Path is a substring from "://" till the end of URL. If there is no "://" URL
   * itself is returned.
   *
   * @param url the URL
   * @return path
   */
  @NotNull
  public static String extractPath(@NotNull String url) {
    int index = url.indexOf(URLUtil.SCHEME_SEPARATOR);
    if (index < 0) return url;
    return url.substring(index + URLUtil.SCHEME_SEPARATOR.length());
  }

  public abstract void notifyPropertyChanged(@NotNull VirtualFile virtualFile, @NotNull String property, Object oldValue, Object newValue);

  /**
   * @return a number that's incremented every time something changes in the VFS, i.e. file hierarchy, names, flags, attributes, contents.
   * This only counts modifications done in current IDE session.
   *  #getStructureModificationCount()
   */
  @Override
  public abstract long getModificationCount();
}
