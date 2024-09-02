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
package com.intellij.openapi.module;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ComponentManager;
import com.intellij.openapi.extensions.AreaInstance;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a module in an IDEA project.
 *
 *  ModuleManager#getModules()
 *  ModuleComponent
 */
public interface Module extends ComponentManager, AreaInstance, Disposable {
  /**
   * The empty array of modules which cab be reused to avoid unnecessary allocations.
   */
  Module[] EMPTY_ARRAY = new Module[0];

  /**
   * Returns the name of this module.
   *
   * @return the module name.
   */
  @NotNull String getName();

  boolean isLoaded();

  /**
   * @return module scope including source and tests, excluding libraries and dependencies.
   */
  @NotNull
  GlobalSearchScope getModuleScope();

  /**
   * @return module scope including source, tests, and libraries, excluding dependencies.
   */
  @NotNull
  GlobalSearchScope getModuleWithLibrariesScope();

  /**
   * @return module scope including source, tests, and dependencies, excluding libraries.
   */
  @NotNull
  GlobalSearchScope getModuleWithDependenciesScope();

  /**
   * @param includeTests whether test source and test dependencies should be included
   * @return a scope including module source and dependencies with libraries
   */
  @NotNull
  GlobalSearchScope getModuleWithDependenciesAndLibrariesScope(boolean includeTests);

  /**
   * @return a scope including everything under the content roots of this module and all modules that depend on it, directly or indirectly (via exported dependencies), excluding test source and resources
   */
  @NotNull
  GlobalSearchScope getModuleWithDependentsScope();

  /**
   * @return same as { #getModuleWithDependentsScope()}, but with test source/resources included
   */
  @NotNull
  GlobalSearchScope getModuleTestsWithDependentsScope();

  /**
   * @param includeTests whether test source and test dependencies should be included
   * @return a scope including production (and optionally test) source of this module and all modules and libraries it depends upon, including runtime and transitive dependencies, even if they're not exported.
   */
  @NotNull
  GlobalSearchScope getModuleRuntimeScope(boolean includeTests);
}
