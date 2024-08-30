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
package com.intellij.util.io;

import com.intellij.util.Processor;

import java.io.IOException;

/**
 * @author Dmitry Avdeev
 *         Date: 8/10/11
 */
public interface PersistentMap<K, V> {
  
  V get(K key) throws IOException;

  void put(K key, V value) throws IOException;

  boolean processKeys(Processor<K> processor) throws IOException;


  boolean isClosed();

  boolean isDirty();

  void force();

  void close() throws IOException;

  void markDirty() throws IOException;
}
