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
package com.intellij.ide.util;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Roaming is disabled for PropertiesComponent, so, use it only and only for temporary non-roamable properties.
 *
 * See http://www.jetbrains.org/intellij/sdk/docs/basics/persisting_state_of_components.html "Using PropertiesComponent for Simple non-roamable Persistence"
 *
 * @author max
 * @author Konstantin Bulenkov
 */
public abstract class PropertiesComponent {

  public abstract boolean isValueSet(String name);

  @Nullable
  public abstract String getValue(@NonNls String name);

  /**
   * Consider to use { #setValue(String, String, String)} to avoid write defaults.
   */
  public abstract void setValue(@NotNull String name, @Nullable String value);

  /**
   * Set value or unset if equals to default value
   */
  public abstract void setValue(@NotNull String name, @Nullable String value, @Nullable String defaultValue);

  /**
   * Set value or unset if equals to default value
   */
  public abstract void setValue(@NotNull String name, float value, float defaultValue);

  /**
   * Set value or unset if equals to default value
   */
  public abstract void setValue(@NotNull String name, int value, int defaultValue);

  /**
   * Set value or unset if equals to false
   */
  public final void setValue(@NotNull String name, boolean value) {
    setValue(name, value, false);
  }

  /**
   * Set value or unset if equals to default
   */
  public abstract void setValue(@NotNull String name, boolean value, boolean defaultValue);

  @Nullable
  public abstract String[] getValues(@NonNls String name);

  public static PropertiesComponent getInstance() {
    return ServiceManager.getService(PropertiesComponent.class);
  }

  public final boolean isTrueValue(@NonNls String name) {
    return Boolean.valueOf(getValue(name)).booleanValue();
  }

  public final boolean getBoolean(@NotNull String name, boolean defaultValue) {
    return isValueSet(name) ? isTrueValue(name) : defaultValue;
  }

  public final boolean getBoolean(@NotNull String name) {
    return getBoolean(name, false);
  }

  @NotNull
  public String getValue(@NonNls String name, @NotNull String defaultValue) {
    if (!isValueSet(name)) {
      return defaultValue;
    }
    return ObjectUtils.notNull(getValue(name), defaultValue);
  }

  /**
   * @deprecated Use { #getInt(String, int)}
   * Init was never performed and in any case is not recommended.
   */
  @Deprecated
  public final int getOrInitInt(@NotNull String name, int defaultValue) {
    return getInt(name, defaultValue);
  }

  public int getInt(@NotNull String name, int defaultValue) {
    return StringUtilRt.parseInt(getValue(name), defaultValue);
  }

  /**
   * @deprecated Use { #getValue(String, String)}
   */
  @Deprecated
  public String getOrInit(@NonNls String name, String defaultValue) {
    if (!isValueSet(name)) {
      setValue(name, defaultValue);
      return defaultValue;
    }
    return getValue(name);
  }
}
