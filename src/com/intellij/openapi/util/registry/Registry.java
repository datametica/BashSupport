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
package com.intellij.openapi.util.registry;

import com.intellij.util.ConcurrencyUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Registry  {
  private static Reference<ResourceBundle> ourBundle;

  @NonNls
  public static final String REGISTRY_BUNDLE = "misc.registry";

  private final Map<String, String> myUserProperties = new LinkedHashMap<String, String>();
  private final ConcurrentMap<String, RegistryValue> myValues = new ConcurrentHashMap<String, RegistryValue>();

  private static final Registry ourInstance = new Registry();

  @NotNull
  public static RegistryValue get(@PropertyKey(resourceBundle = REGISTRY_BUNDLE) @NotNull String key) {
    final Registry registry = getInstance();

    RegistryValue value = registry.myValues.get(key);
    if (value == null) {
      value = ConcurrencyUtil.cacheOrGet(registry.myValues, key, new RegistryValue(registry, key));
    }
    return value;
  }

  public static boolean is(@PropertyKey(resourceBundle = REGISTRY_BUNDLE) @NotNull String key) throws MissingResourceException {
    return get(key).asBoolean();
  }

  public static boolean is(@PropertyKey(resourceBundle = REGISTRY_BUNDLE) @NotNull String key, boolean defaultValue) {
    try {
      return get(key).asBoolean();
    }
    catch (MissingResourceException ex) {
      return defaultValue;
    }
  }

  public static int intValue(@PropertyKey(resourceBundle = REGISTRY_BUNDLE) @NotNull String key) throws MissingResourceException {
    return get(key).asInteger();
  }

  public static int intValue(@PropertyKey(resourceBundle = REGISTRY_BUNDLE) @NotNull String key, int defaultValue) {
    try {
      return get(key).asInteger();
    }
    catch (MissingResourceException ex) {
      return defaultValue;
    }
  }

  @NotNull
  static ResourceBundle getBundle() {
    ResourceBundle bundle = com.intellij.reference.SoftReference.dereference(ourBundle);
    if (bundle == null) {
      bundle = ResourceBundle.getBundle(REGISTRY_BUNDLE);
      ourBundle = new SoftReference<ResourceBundle>(bundle);
    }
    return bundle;
  }


  @NotNull
  public static Registry getInstance() {
    return ourInstance;
  }

  @NotNull
  public Element getState() {
    final Element state = new Element("registry");
    for (String eachKey : myUserProperties.keySet()) {
      final Element entry = new Element("entry");
      entry.setAttribute("key", eachKey);
      entry.setAttribute("value", myUserProperties.get(eachKey));
      state.addContent(entry);
    }
    return state;
  }

  @NotNull
  Map<String, String> getUserProperties() {
    return myUserProperties;
  }

  @NotNull
  public static List<RegistryValue> getAll() {
    final ResourceBundle bundle = getBundle();
    final Enumeration<String> keys = bundle.getKeys();

    List<RegistryValue> result = new ArrayList<RegistryValue>();

    while (keys.hasMoreElements()) {
      final String each = keys.nextElement();
      if (each.endsWith(".description") || each.endsWith(".restartRequired")) continue;
      result.add(get(each));
    }

    return result;
  }
}
