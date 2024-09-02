/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.openapi.roots;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author yole
 */
public abstract class FileIndexFacade {

  protected FileIndexFacade() {

  }

  public static FileIndexFacade getInstance() {
    return null;
  }

  public abstract boolean isInContent(@NotNull VirtualFile file);
  public abstract boolean isInLibraryClasses(@NotNull VirtualFile file);

  public abstract boolean isInLibrarySource(@NotNull VirtualFile file);
  public abstract boolean isExcludedFile(@NotNull VirtualFile file);
  public abstract boolean isUnderIgnored(@NotNull VirtualFile file);

  @Nullable
  public abstract Module getModuleForFile(@NotNull VirtualFile file);
}
