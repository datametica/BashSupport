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
package com.intellij;

import com.intellij.openapi.util.SystemInfo;



@SuppressWarnings({"HardCodedStringLiteral", "UtilityClassWithoutPrivateConstructor"})
public class Patches {
  /**
   * Marker field to find all usages of the reflective access to JDK 7-specific methods
   * which need to be changed when migrated to JDK 7
   */
  public static final boolean USE_REFLECTION_TO_ACCESS_JDK7 = Boolean.valueOf(true);

  /**
   * Marker field to find all usages of the reflective access to JDK 7-specific methods
   * which need to be changed when migrated to JDK 8
   */
  public static final boolean USE_REFLECTION_TO_ACCESS_JDK8 = Boolean.valueOf(true);

  /**
   * On Mac OS font ligatures are not supported for natively loaded fonts, font needs to be loaded explicitly by JDK. 
   */
  public static final boolean JDK_BUG_ID_7162125;
  static {
    boolean value;
    if (!SystemInfo.isMac || SystemInfo.isJavaVersionAtLeast("1.9")) value = false;
    else if (!SystemInfo.isJetbrainsJvm) value = true;
    else {
      try {
        Class.forName("sun.font.CCompositeFont");
        value = Boolean.getBoolean("disable.font.substitution");
      }
      catch (Throwable e) {
        value = true;
      }
    }
    JDK_BUG_ID_7162125 = value;
  }
}