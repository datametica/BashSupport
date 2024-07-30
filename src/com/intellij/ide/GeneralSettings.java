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
package com.intellij.ide;

import com.intellij.openapi.components.ServiceManager;

import java.beans.PropertyChangeSupport;

public class GeneralSettings {

  private int myInactiveTimeout; // Number of seconds of inactivity after which IDEA automatically saves all files
  private boolean myUseSafeWrite = true;
  private final PropertyChangeSupport myPropertyChangeSupport;

  public static GeneralSettings getInstance(){
    return ServiceManager.getService(GeneralSettings.class);
  }

  public GeneralSettings() {
    myInactiveTimeout = 15;
    myPropertyChangeSupport = new PropertyChangeSupport(this);
  }

  public boolean isUseSafeWrite() {
    return myUseSafeWrite;
  }
}
