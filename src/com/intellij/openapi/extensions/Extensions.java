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
package com.intellij.openapi.extensions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Extensions {

  private Extensions() {
  }

  @NotNull
  public static ExtensionsArea getRootArea() {
    return null/*ourRootArea*/;
  }

  @NotNull
  public static ExtensionsArea getArea(@Nullable("null means root") AreaInstance areaInstance) {
    if (areaInstance == null) {
      return null/*ourRootArea*/;
    }
    return null;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public static <T> T[] getExtensions(@NotNull ExtensionPointName<T> extensionPointName) {
    return (T[])getExtensions(extensionPointName.getName(), null);
  }

  @NotNull
  public static <T> T[] getExtensions(String extensionPointName, @Nullable("null means root") AreaInstance areaInstance) {
    ExtensionsArea area = getArea(areaInstance);
    ExtensionPoint<T> extensionPoint = area.getExtensionPoint(extensionPointName);
    return extensionPoint.getExtensions();
  }
}
