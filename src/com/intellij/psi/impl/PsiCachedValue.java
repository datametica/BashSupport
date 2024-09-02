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

package com.intellij.psi.impl;

import com.intellij.util.CachedValueBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Dmitry Avdeev
 */
public abstract class PsiCachedValue<T> extends CachedValueBase<T> {

  protected volatile long myLastPsiTimeStamp = -1;


  @Override
  protected void valueUpdated(@Nullable Object[] dependencies) {
    //myLastPsiTimeStamp = hasOnlyPhysicalPsiDependencies(dependencies) ? myManager.getModificationTracker().getModificationCount() : -1;
  }

  @Nullable
  @Override
  protected <P> T getValueWithLock(P param) {
    return super.getValueWithLock(param);
  }

  @Override
  protected boolean isUpToDate(@NotNull Data data) {
    return /*!myManager.isDisposed() && super.isUpToDate(data)*/false;
  }

  @Override
  protected boolean isDependencyOutOfDate(Object dependency, long oldTimeStamp) {
    /*if (myLastPsiTimeStamp != -1 && myLastPsiTimeStamp == myManager.getModificationTracker().getModificationCount()) {
      return false;
    }*/

    return super.isDependencyOutOfDate(dependency, oldTimeStamp);
  }

  @Override
  protected long getTimeStamp(Object dependency) {

/*
    if (dependency instanceof PsiDirectory) {
      return myManager.getModificationTracker().getOutOfCodeBlockModificationCount();
    }

    if (dependency instanceof PsiElement) {
      PsiElement element = (PsiElement)dependency;
      if (!element.isValid()) return -1;
      PsiFile containingFile = element.getContainingFile();
      if (containingFile != null) return containingFile.getModificationStamp();
    }

    if (dependency == PsiModificationTracker.MODIFICATION_COUNT) {
      return myManager.getModificationTracker().getModificationCount();
    }
    if (dependency == PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT) {
      return myManager.getModificationTracker().getOutOfCodeBlockModificationCount();
    }
    if (dependency == PsiModificationTracker.JAVA_STRUCTURE_MODIFICATION_COUNT) {
      return myManager.getModificationTracker().getJavaStructureModificationCount();
    }
*/

    return super.getTimeStamp(dependency);
  }

}
