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
package com.intellij.util.indexing;

import com.intellij.openapi.components.BaseComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.SystemProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Author: dmitrylomov
 */
public abstract class FileBasedIndex implements BaseComponent {


  public static FileBasedIndex getInstance() {
    return null;
  }

  @NotNull
  public abstract <K, V> List<V> getValues(@NotNull ID<K, V> indexId, @NotNull K dataKey, @NotNull GlobalSearchScope filter);

  @NotNull
  public abstract <K, V> Collection<VirtualFile> getContainingFiles(@NotNull ID<K, V> indexId,
                                                                    @NotNull K dataKey,
                                                                    @NotNull GlobalSearchScope filter);

  /**
   * @return false if ValueProcessor.process() returned false; true otherwise or if ValueProcessor was not called at all
   */
  public abstract <K, V> boolean processValues(@NotNull ID<K, V> indexId,
                                               @NotNull K dataKey,
                                               @Nullable VirtualFile inFile,
                                               @NotNull FileBasedIndex.ValueProcessor<V> processor,
                                               @NotNull GlobalSearchScope filter);

  /**
   * @return false if ValueProcessor.process() returned false; true otherwise or if ValueProcessor was not called at all
   */
  public <K, V> boolean processValues(@NotNull ID<K, V> indexId,
                                               @NotNull K dataKey,
                                               @Nullable VirtualFile inFile,
                                               @NotNull FileBasedIndex.ValueProcessor<V> processor,
                                               @NotNull GlobalSearchScope filter,
                                               @Nullable IdFilter idFilter) {
    return processValues(indexId, dataKey, inFile, processor, filter);
  }

  /**
   *  project it is guaranteed to return data which is up-to-date withing the project
   *                Keys obtained from the files which do not belong to the project specified may not be up-to-date or even exist
   */
  @NotNull
  public abstract <K> Collection<K> getAllKeys(@NotNull ID<K, ?> indexId);

  /**
   * it is guaranteed to return data which is up-to-date withing the project
   *                Keys obtained from the files which do not belong to the project specified may not be up-to-date or even exist
   */
  public abstract <K> boolean processAllKeys(@NotNull ID<K, ?> indexId, @NotNull Processor<K> processor);

  public <K> boolean processAllKeys(@NotNull ID<K, ?> indexId, @NotNull Processor<K> processor, @NotNull GlobalSearchScope scope, @Nullable IdFilter idFilter) {
    return processAllKeys(indexId, processor);
  }

  @FunctionalInterface
  public interface ValueProcessor<V> {
    /**
     * @param value a value to process
     * @param file the file the value came from
     * @return false if no further processing is needed, true otherwise
     */
    boolean process(@NotNull VirtualFile file, V value);
  }

  @FunctionalInterface
  public interface InputFilter {
    boolean acceptInput(@NotNull VirtualFile file);
  }

  // TODO: remove once changes becomes permanent
  public static final boolean ourEnableTracingOfKeyHashToVirtualFileMapping =
    SystemProperties.getBooleanProperty("idea.enable.tracing.keyhash2virtualfile", true);
}
