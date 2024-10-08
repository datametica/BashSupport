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
package com.intellij.openapi.fileTypes;

import com.intellij.openapi.util.Getter;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public abstract class FileTypeRegistry {
  public static Getter<FileTypeRegistry> ourInstanceGetter;

  public abstract boolean isFileIgnored(@NotNull VirtualFile file);

  public static FileTypeRegistry getInstance() {
    return ourInstanceGetter.get();
  }

  /**
   * Returns the list of all registered file types.
   *
   * @return The list of file types.
   */
  public abstract FileType[] getRegisteredFileTypes();

  /**
   * Returns the file type for the specified file.
   *
   * @param file The file for which the type is requested.
   * @return The file type instance.
   */
  @NotNull
  public abstract FileType getFileTypeByFile(@NotNull VirtualFile file);

  /**
   * Returns the file type for the specified file name.
   *
   * @param fileName The file name for which the type is requested.
   * @return The file type instance, or { FileTypes#UNKNOWN} if not found.
   */
  @NotNull
  public abstract FileType getFileTypeByFileName(@NotNull @NonNls String fileName);

  /**
   * Tries to detect whether the file is text or not by analyzing its content.
   * @param file to analyze
   * @return { com.intellij.openapi.fileTypes.PlainTextFileType} if file looks like text,
   *          or another file type if some file type detector identified the file
   *          or the { UnknownFileType} if file is binary or we are unable to detect.
   * @deprecated use { VirtualFile#getFileType()} instead
   */
  @NotNull
  @Deprecated
  public abstract FileType detectFileTypeFromContent(@NotNull VirtualFile file);
}
