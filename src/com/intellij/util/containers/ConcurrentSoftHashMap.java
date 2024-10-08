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

/*
 * Created by IntelliJ IDEA.
 * User: Alexey
 * Date: 18.12.2006
 * Time: 20:18:31
 */
package com.intellij.util.containers;

import gnu.trove.TObjectHashingStrategy;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Map;

/**
 * Concurrent soft key:K -> strong value:V map
 * Null keys are allowed
 * Null values are NOT allowed
 * @deprecated Use { ContainerUtil#createConcurrentSoftMap()} instead
 */
@Deprecated
final class ConcurrentSoftHashMap<K, V> extends ConcurrentRefHashMap<K, V> {
  private static class SoftKey<K, V> extends SoftReference<K> implements KeyReference<K, V> {
    private final int myHash; // Hashcode of key, stored here since the key may be tossed by the GC
    private final TObjectHashingStrategy<K> myStrategy;
    private final V value;

    private SoftKey(@NotNull K k, final int hash, @NotNull TObjectHashingStrategy<K> strategy, V v, @NotNull ReferenceQueue<K> q) {
      super(k, q);
      myStrategy = strategy;
      value = v;
      myHash = hash;
    }

    @NotNull
    @Override
    public V getValue() {
      return value;
    }

    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof KeyReference)) return false;
      K t = get();
      K u = ((KeyReference<K,V>)o).get();
      if (t == u) return true;
      if (t == null || u == null) return false;
      return myStrategy.equals(t, u);
    }

    public int hashCode() {
      return myHash;
    }
  }

  @Override
  protected KeyReference<K, V> createKeyReference(@NotNull K key, @NotNull V value, @NotNull TObjectHashingStrategy<K> hashingStrategy) {
    return new SoftKey<K, V>(key, hashingStrategy.computeHashCode(key), hashingStrategy, value, myReferenceQueue);
  }

  public ConcurrentSoftHashMap(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  public ConcurrentSoftHashMap(int initialCapacity) {
    super(initialCapacity);
  }

  public ConcurrentSoftHashMap() {
  }

  public ConcurrentSoftHashMap(int initialCapacity,
                               float loadFactor,
                               int concurrencyLevel,
                               @NotNull TObjectHashingStrategy<K> hashingStrategy) {
    super(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
  }

  public ConcurrentSoftHashMap(Map<? extends K, ? extends V> t) {
    super(t);
  }

  public ConcurrentSoftHashMap(@NotNull TObjectHashingStrategy<K> hashingStrategy) {
    super(hashingStrategy);
  }
}
