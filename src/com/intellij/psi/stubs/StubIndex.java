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

/*
 * @author max
 */
package com.intellij.psi.stubs;

import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.Processors;
import com.intellij.util.SmartList;
import com.intellij.util.indexing.IdFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public abstract class StubIndex {
  private static class StubIndexHolder {
    private static final StubIndex ourInstance = null;
  }
  public static StubIndex getInstance() {
    return StubIndexHolder.ourInstance;
  }

  /**
   * @deprecated use { #getElements(StubIndexKey, Object, com.intellij.openapi.project.Project, GlobalSearchScope, Class)}
   */
  @Deprecated
  public abstract <Key, Psi extends PsiElement> Collection<Psi> get(@NotNull StubIndexKey<Key, Psi> indexKey,
                                                                    @NotNull Key key,
                                                                    @Nullable final GlobalSearchScope scope);

  /**
   * @deprecated use { #getElements(StubIndexKey, Object, com.intellij.openapi.project.Project, GlobalSearchScope, Class)}
   */
  @Deprecated
  public <Key, Psi extends PsiElement> Collection<Psi> get(@NotNull StubIndexKey<Key, Psi> indexKey,
                                                                    @NotNull Key key,
                                                                    @Nullable final GlobalSearchScope scope,
                                                                    IdFilter filter) {
    return get(indexKey, key,  scope);
  }

  /**
   * @deprecated use processElements
   */
  @Deprecated
  public <Key, Psi extends PsiElement> boolean process(@NotNull StubIndexKey<Key, Psi> indexKey,
                                                                @NotNull Key key,
                                                                @Nullable GlobalSearchScope scope,
                                                                @NotNull Processor<? super Psi> processor) {
    return processElements(indexKey, key, scope, (Class<Psi>)PsiElement.class, processor);
  }

  public abstract <Key, Psi extends PsiElement> boolean processElements(@NotNull StubIndexKey<Key, Psi> indexKey,
                                                                @NotNull Key key,
                                                                @Nullable GlobalSearchScope scope,
                                                                Class<Psi> requiredClass,
                                                                @NotNull Processor<? super Psi> processor);

  /**
   * @deprecated use processElements
   */
  @Deprecated
  public <Key, Psi extends PsiElement> boolean process(@NotNull StubIndexKey<Key, Psi> indexKey,
                                                                @NotNull Key key,
                                                                @Nullable GlobalSearchScope scope,
                                                                @SuppressWarnings("UnusedParameters") IdFilter idFilter,
                                                                @NotNull Processor<? super Psi> processor) {
    return process(indexKey, key, scope, processor);
  }

  public <Key, Psi extends PsiElement> boolean processElements(@NotNull StubIndexKey<Key, Psi> indexKey,
                                                                @NotNull Key key,
                                                                @Nullable GlobalSearchScope scope,
                                                                IdFilter idFilter,
                                                                @NotNull Class<Psi> requiredClass,
                                                                @NotNull Processor<? super Psi> processor) {
    return process(indexKey, key, scope, processor);
  }

  /**
   * @deprecated use { #getElements(StubIndexKey, Object, com.intellij.openapi.project.Project, GlobalSearchScope, Class)}
   */
  @Deprecated
  public <Key, Psi extends PsiElement> Collection<Psi> safeGet(@NotNull StubIndexKey<Key, Psi> indexKey,
                                                               @NotNull Key key,
                                                               final GlobalSearchScope scope,
                                                               @NotNull Class<Psi> requiredClass) {
    return getElements(indexKey, key,  scope, requiredClass);
  }

  public static <Key, Psi extends PsiElement> Collection<Psi> getElements(@NotNull StubIndexKey<Key, Psi> indexKey,
                                                                          @NotNull Key key,
                                                                          @Nullable final GlobalSearchScope scope,
                                                                          @NotNull Class<Psi> requiredClass) {
    return getElements(indexKey, key, scope, null, requiredClass);
  }

  public static <Key, Psi extends PsiElement> Collection<Psi> getElements(@NotNull StubIndexKey<Key, Psi> indexKey,
                                                                          @NotNull Key key,
                                                                          @Nullable final GlobalSearchScope scope,
                                                                          @Nullable IdFilter idFilter,
                                                                          @NotNull Class<Psi> requiredClass) {
    final List<Psi> result = new SmartList<>();
    Processor<Psi> processor = Processors.cancelableCollectProcessor(result);
    getInstance().processElements(indexKey, key,  scope, idFilter, requiredClass, processor);
    return result;
  }
}
