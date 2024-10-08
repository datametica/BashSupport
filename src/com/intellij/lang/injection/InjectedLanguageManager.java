/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
 * Created by IntelliJ IDEA.
 * User: cdr
 * Date: Jun 8, 2007
 * Time: 8:41:25 PM
 */
package com.intellij.lang.injection;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLanguageInjectionHost;
import org.jetbrains.annotations.NotNull;

public abstract class InjectedLanguageManager {

  /**  com.intellij.lang.injection.MultiHostInjector#MULTIHOST_INJECTOR_EP_NAME */
  @Deprecated
  public static final ExtensionPointName<MultiHostInjector> MULTIHOST_INJECTOR_EP_NAME = MultiHostInjector.MULTIHOST_INJECTOR_EP_NAME;

  public static InjectedLanguageManager getInstance() {
    return null;
  }

  /**
   * Test-only method.
   *  com.intellij.lang.injection.MultiHostInjector#MULTIHOST_INJECTOR_EP_NAME
   */
  @Deprecated
  public abstract void registerMultiHostInjector(@NotNull MultiHostInjector injector);

  /**
   * Test-only method.
   *  com.intellij.lang.injection.MultiHostInjector#MULTIHOST_INJECTOR_EP_NAME
   */
  @Deprecated
  public abstract boolean unregisterMultiHostInjector(@NotNull MultiHostInjector injector);

  public abstract boolean isInjectedFragment(@NotNull PsiFile file);

  public abstract void enumerate(@NotNull PsiElement host, @NotNull PsiLanguageInjectionHost.InjectedPsiVisitor visitor);

}
