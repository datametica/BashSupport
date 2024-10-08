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

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;



public class UnknownFileType implements FileType {
  public static final FileType INSTANCE = new UnknownFileType();

  private UnknownFileType() {}

  @Override
  @NotNull
  public String getName() {
    return "UNKNOWN";
  }

  @Override
  @NotNull
  public String getDescription() {
    return "UNKNOWN";
  }

  @Override
  @NotNull
  public String getDefaultExtension() {
    return "";
  }

  @Override
  public boolean isBinary() {
    return true;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public String getCharset(@NotNull VirtualFile file, @NotNull final byte[] content) {
    return null;
  }
}
