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

/*
 * @author max
 */
package com.intellij.openapi.util;

import com.intellij.diagnostic.PluginException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.*;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.KeyedLazyInstance;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

public class KeyedExtensionCollector<T, KeyT> {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.util.KeyedExtensionCollector");

  private final Map<String, List<T>> myExplicitExtensions = new THashMap<String, List<T>>();
  private final ConcurrentMap<String, List<T>> myCache = ContainerUtil.newConcurrentMap();

  @NonNls private final String lock;

  private ExtensionPoint<KeyedLazyInstance<T>> myPoint;
  private final String myEpName;
  private ExtensionPointAndAreaListener<KeyedLazyInstance<T>> myListener;
  private final List<ExtensionPointListener<T>> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

  public KeyedExtensionCollector(@NonNls @NotNull String epName) {
    myEpName = epName;
    lock = "lock for KeyedExtensionCollector " + epName;
    resetAreaListener();
  }

  private void resetAreaListener() {
    synchronized (lock) {
      myCache.clear();

      if (myPoint != null) {
        myPoint.removeExtensionPointListener(myListener);
        myPoint = null;
        myListener = null;
      }
    }
  }

  @NotNull
  protected String keyToString(@NotNull KeyT key) {
    return key.toString();
  }

  /**
   *  #findSingle(Object)
   */
  @NotNull
  public List<T> forKey(@NotNull KeyT key) {
    final String stringKey = keyToString(key);

    boolean rebuild = false;//myPoint == null && Extensions.getRootArea().hasExtensionPoint(myEpName);
    List<T> cached = rebuild ? null : myCache.get(stringKey);
    if (cached != null) return cached;

    cached = buildExtensions(stringKey, key);
    cached = ConcurrencyUtil.cacheOrGet(myCache, stringKey, cached);
    return cached;
  }

  @NotNull
  protected List<T> buildExtensions(@NotNull String stringKey, @NotNull KeyT key) {
    return buildExtensions(Collections.singleton(stringKey));
  }

  @NotNull
  protected final List<T> buildExtensions(@NotNull Set<String> keys) {
    synchronized (lock) {
      List<T> result = null;
      for (Map.Entry<String, List<T>> entry : myExplicitExtensions.entrySet()) {
        String key = entry.getKey();
        if (keys.contains(key)) {
          List<T> list = entry.getValue();
          if (result == null) {
            result = new ArrayList<T>(list);
          }
          else {
            result.addAll(list);
          }
        }
      }

      final ExtensionPoint<KeyedLazyInstance<T>> point = getPoint();
      if (point != null) {
        final KeyedLazyInstance<T>[] beans = point.getExtensions();
        for (KeyedLazyInstance<T> bean : beans) {
          if (keys.contains(bean.getKey())) {
            final T instance;
            try {
              instance = bean.getInstance();
            }
            catch (ProcessCanceledException e) {
              throw e;
            }
            catch (Exception e) {
              LOG.error(e);
              continue;
            }
            catch (LinkageError e) {
              LOG.error(e);
              continue;
            }
            if (result == null) result = new SmartList<T>();
            result.add(instance);
          }
        }
      }
      return result == null ? Collections.<T>emptyList() : result;
    }
  }

  @Nullable
  private ExtensionPoint<KeyedLazyInstance<T>> getPoint() {
    ExtensionPoint<KeyedLazyInstance<T>> point = myPoint;
    if (point == null && false/*Extensions.getRootArea().hasExtensionPoint(myEpName)*/) {
      ExtensionPointName<KeyedLazyInstance<T>> typesafe = ExtensionPointName.create(myEpName);
      myPoint = point = Extensions.getRootArea().getExtensionPoint(typesafe);
      myListener = new ExtensionPointAndAreaListener<KeyedLazyInstance<T>>() {
        @Override
        public void extensionAdded(@NotNull final KeyedLazyInstance<T> bean, @Nullable final PluginDescriptor pluginDescriptor) {
          synchronized (lock) {
            if (bean.getKey() == null) {
              if (pluginDescriptor != null) {
                throw new PluginException("No key specified for extension of class " + bean.getInstance().getClass(),
                                          pluginDescriptor.getPluginId());
              }
              LOG.error("No key specified for extension of class " + bean.getInstance().getClass());
              return;
            }
            myCache.remove(bean.getKey());
            for (ExtensionPointListener<T> listener : myListeners) {
              listener.extensionAdded(bean.getInstance(), null);
            }
          }
        }

        @Override
        public void extensionRemoved(@NotNull final KeyedLazyInstance<T> bean, @Nullable final PluginDescriptor pluginDescriptor) {
          synchronized (lock) {
            myCache.remove(bean.getKey());
            for (ExtensionPointListener<T> listener : myListeners) {
              listener.extensionRemoved(bean.getInstance(), null);
            }
          }
        }
      };

      point.addExtensionPointListener(myListener);
    }
    return point;
  }
}
