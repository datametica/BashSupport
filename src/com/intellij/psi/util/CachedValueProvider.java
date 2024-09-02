/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.psi.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * A computation (typically an anonymous class) to used in { CachedValue} to cache some computation result.
 * @param <T> the type of the cached value
 */
public interface CachedValueProvider<T> {

  /**
   * @return result object holding the value to cache and the dependencies indicating when that value will be outdated
   */
  @Nullable
  Result<T> compute();

  /**
   * The object holding the value to cache and the dependencies indicating when that value will be outdated
   * @param <T> the type of the cached value
   */
  class Result<T> {
    private static final Logger LOG = Logger.getInstance("#com.intellij.psi.util.CachedValueProvider.Result");
    private final T myValue;
    private final Object[] myDependencyItems;

    /**
     * Constructor
     *  #getDependencyItems()
     */
    public Result(@Nullable T value, @NotNull Object... dependencyItems) {
      myValue = value;
      myDependencyItems = dependencyItems;

      if (dependencyItems.length == 0) {
        LOG.error("No dependencies provided which causes CachedValue to be never recalculated again. " +
                  "If this is intentional, please use ModificationTracker.NEVER_CHANGED");
      }
      for (int i = 0; i < dependencyItems.length; i++) {
        if (dependencyItems[i] == null) {
          LOG.error("Null dependencies are not allowed, index=" + i);
        }
      }
    }

    public T getValue() {
      return myValue;
    }

    /**
     * Dependency items are used in cached values to remember the state of the environment as it was when the value was computed
     * and to compare that to the state of the world when querying { CachedValue#getValue()}. The state is remembered as
     * a collection of {@code long} values representing some time stamps. When changes occur, these stamps are incremented.<p/>
     *
     * Dependencies can be following:
     * <ul>
     *   <li/>Instances of { com.intellij.openapi.util.ModificationTracker} returning stamps explicitly
     *   <li/>Constant fields of { PsiModificationTracker} class, e.g. { PsiModificationTracker#MODIFICATION_COUNT}
     *   <li/>{ com.intellij.psi.PsiElement} or { com.intellij.openapi.vfs.VirtualFile} objects. Such cache would be dropped
     *   on any change in the corresponding file
     * </ul>
     *
     * @return the dependency items
     *  com.intellij.openapi.util.ModificationTracker
     *  PsiModificationTracker
     *  com.intellij.openapi.roots.ProjectRootModificationTracker
     */
    @NotNull
    public Object[] getDependencyItems() {
      return myDependencyItems;
    }

    /**
     * Creates a result
     *  #getDependencyItems()
     */
    public static <T> Result<T> create(@Nullable T value, @NotNull Object... dependencies) {
      return new Result<T>(value, dependencies);
    }

    /**
     * Creates a result
     *  #getDependencyItems()
     */
    public static <T> Result<T> create(@Nullable T value, @NotNull Collection<?> dependencies) {
      return new Result<T>(value, ArrayUtil.toObjectArray(dependencies));
    }

  }
}
