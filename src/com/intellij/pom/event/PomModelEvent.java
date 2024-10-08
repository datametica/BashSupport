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
package com.intellij.pom.event;

import com.intellij.pom.PomModel;
import com.intellij.pom.PomModelAspect;
import org.jetbrains.annotations.NotNull;

import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public class PomModelEvent extends EventObject {
  private Map<PomModelAspect, PomChangeSet> myChangeSets;

  public PomModelEvent(PomModel source) {
    super(source);
  }

  public void merge(@NotNull PomModelEvent event) {
    if(event.myChangeSets == null) return;
    if(myChangeSets == null){
      myChangeSets = new HashMap<PomModelAspect, PomChangeSet>(event.myChangeSets);
      return;
    }
    for (final Map.Entry<PomModelAspect, PomChangeSet> entry : event.myChangeSets.entrySet()) {
      final PomModelAspect aspect = entry.getKey();
      final PomChangeSet pomChangeSet = myChangeSets.get(aspect);
      if (pomChangeSet != null) {
        pomChangeSet.merge(entry.getValue());
      }
      else {
        myChangeSets.put(aspect, entry.getValue());
      }
    }
  }


  @Override
  public PomModel getSource() {
    return (PomModel)super.getSource();
  }
}
