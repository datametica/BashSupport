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

package com.intellij.psi.impl.source.resolve;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.openapi.util.Trinity;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.util.containers.ConcurrentWeakKeySoftValueHashMap;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.ReferenceQueue;
import java.util.concurrent.ConcurrentMap;

public class ResolveCache {
  private static final Logger LOG = Logger.getInstance("#com.intellij.psi.impl.source.resolve.ResolveCache");
  @SuppressWarnings("unchecked")
  private final ConcurrentMap[] myMaps = new ConcurrentWeakKeySoftValueHashMap[2*2*2]; //boolean physical, boolean incompleteCode, boolean isPoly
  private final RecursionGuard myGuard = RecursionManager.createGuard("resolveCache");

  public static ResolveCache getInstance() {
    ProgressIndicatorProvider.checkCanceled(); // We hope this method is being called often enough to cancel daemon processes smoothly
    return ServiceManager.getService( ResolveCache.class);
  }

  public interface AbstractResolver<TRef extends PsiReference, TResult> {
    TResult resolve(@NotNull TRef ref, boolean incompleteCode);
  }

  /**
   * Resolver specialized to resolve PsiReference to PsiElement
   */
  public interface Resolver extends AbstractResolver<PsiReference, PsiElement> {
  }

  public ResolveCache(@NotNull MessageBus messageBus) {
    for (int i = 0; i < myMaps.length; i++) {
      myMaps[i] = createWeakMap();
    }
  }

  @NotNull
  private static <K,V> ConcurrentMap<K, V> createWeakMap() {
    return new ConcurrentWeakKeySoftValueHashMap<K, V>(100, 0.75f, Runtime.getRuntime().availableProcessors(), ContainerUtil.<K>canonicalStrategy()){
      @NotNull
      @Override
      protected ValueReference<K, V> createValueReference(@NotNull final V value,
                                                          @NotNull ReferenceQueue<V> queue) {
        ValueReference<K, V> result;
        if (value == NULL_RESULT || value instanceof Object[] && ((Object[])value).length == 0) {
          // no use in creating SoftReference to null
          result = createStrongReference(value);
        }
        else {
          result = super.createValueReference(value, queue);
        }
        return result;
      }

      @Override
      public V get(@NotNull Object key) {
        V v = super.get(key);
        return v == NULL_RESULT ? null : v;
      }
    };
  }

  @Nullable
  private <TRef extends PsiReference, TResult> TResult resolve(@NotNull final TRef ref,
                                                               @NotNull final AbstractResolver<TRef, TResult> resolver,
                                                               boolean needToPreventRecursion,
                                                               final boolean incompleteCode,
                                                               boolean isPoly,
                                                               boolean isPhysical) {
    ProgressIndicatorProvider.checkCanceled();
    if (isPhysical) {

    }

    int index = getIndex(isPhysical, incompleteCode, isPoly);
    ConcurrentMap<TRef, TResult> map = getMap(index);
    TResult result = map.get(ref);
    if (result != null) {
      return result;
    }

    RecursionGuard.StackStamp stamp = myGuard.markStack();
    result = needToPreventRecursion ? myGuard.doPreventingRecursion(Trinity.create(ref, incompleteCode, isPoly), true, new Computable<TResult>() {
      @Override
      public TResult compute() {
        return resolver.resolve(ref, incompleteCode);
      }
    }) : resolver.resolve(ref, incompleteCode);
    PsiElement element = result instanceof ResolveResult ? ((ResolveResult)result).getElement() : null;
    LOG.assertTrue(element == null || element.isValid(), result);

    if (stamp.mayCacheNow()) {
      cache(ref, map, result);
    }
    return result;
  }

  @Nullable
  public <TRef extends PsiReference, TResult>
         TResult resolveWithCaching(@NotNull TRef ref,
                                    @NotNull AbstractResolver<TRef, TResult> resolver,
                                    boolean needToPreventRecursion,
                                    boolean incompleteCode) {
    return resolve(ref, resolver, needToPreventRecursion, incompleteCode, false, ref.getElement().isPhysical());
  }

  @NotNull
  private <TRef extends PsiReference,TResult> ConcurrentMap<TRef, TResult> getMap(int index) {
    //noinspection unchecked
    return myMaps[index];
  }

  private static int getIndex(boolean physical, boolean incompleteCode, boolean isPoly) {
    return (physical ? 0 : 1)*4 + (incompleteCode ? 0 : 1)*2 + (isPoly ? 0 : 1);
  }

  private static final Object NULL_RESULT = new Object();
  private <TRef extends PsiReference, TResult> void cache(@NotNull TRef ref,
                                                          @NotNull ConcurrentMap<TRef, TResult> map,
                                                          TResult result) {
    // optimization: less contention
    TResult cached = map.get(ref);
    if (cached != null && cached == result) {
      return;
    }
    if (result == null) {
      // no use in creating SoftReference to null
      //noinspection unchecked
      cached = (TResult)NULL_RESULT;
    }
    else {
      //noinspection unchecked
      cached = result;
    }
    map.put(ref, cached);
  }

  @NotNull
  private static <K, V> StrongValueReference<K, V> createStrongReference(@NotNull V value) {
    return value == NULL_RESULT ? NULL_VALUE_REFERENCE : value == ResolveResult.EMPTY_ARRAY ? EMPTY_RESOLVE_RESULT : new StrongValueReference<K, V>(value);
  }

  private static final StrongValueReference NULL_VALUE_REFERENCE = new StrongValueReference(NULL_RESULT);
  private static final StrongValueReference EMPTY_RESOLVE_RESULT = new StrongValueReference(ResolveResult.EMPTY_ARRAY);
  private static class StrongValueReference<K, V> implements ConcurrentWeakKeySoftValueHashMap.ValueReference<K, V> {
    private final V myValue;

    public StrongValueReference(@NotNull V value) {
      myValue = value;
    }

    @NotNull
    @Override
    public ConcurrentWeakKeySoftValueHashMap.KeyReference<K, V> getKeyReference() {
      throw new UnsupportedOperationException(); // will never GC so this method will never be called so no implementation is necessary
    }

    @Override
    public V get() {
      return myValue;
    }
  }
}
