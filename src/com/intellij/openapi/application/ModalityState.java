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
package com.intellij.openapi.application;

import org.jetbrains.annotations.NotNull;



/**
 * Represents the stack of active modal dialogs. Used in calls to { Application#invokeAndWait(Runnable, ModalityState)} to specify
 * that the corresponding runnable is to be executed within the given modality state, i.e. when the same set modal dialogs is present, or its subset.<p/>
 *
 * Modality state is used to prevent the following scenario. Someone does SwingUtilities.invokeAndWait, but there are already other runnables in
 * Swing queue, so they are executed before and show a dialog (e.g. asking a yes/no question). While this dialog is shown, further events are pumped
 * from the queue, including the one scheduled before, which does something very dramatic, e.g. removes a module from the project, deletes some files,
 * invalidates PSI. It's executed, and only then the user closes the dialog. The code that invoked that dialog now has to deal with the completely
 * changed world, where PSI that it worked with might be already invalid, dumb mode (see { com.intellij.openapi.project.DumbService})
 * might have unexpectedly begun, etc. But normally clients of yes/no question dialogs aren't prepared to this at all, so exceptions are likely to arise.
 * Worse than that, there'll be no indication on why a particular change has occurred, because the runnable that was incorrectly invoked-later will
 * in many cases leave no trace of itself.<p/>
 *
 * For these reasons, it's strongly advised to use { Application#invokeAndWait(Runnable, ModalityState)} everywhere.
 * { javax.swing.SwingUtilities#invokeLater(Runnable)} and { com.intellij.util.ui.UIUtil} convenience methods can be used in the
 * pure UI code, but not with anything that deals with PSI or VFS.
 */
public abstract class ModalityState {
  /**
   * State when no modal dialogs are open.
   *  Application#getNoneModalityState()
   */
  @NotNull public static final ModalityState NON_MODAL;

  static {
    try {
      @SuppressWarnings("unchecked")
      final Class<? extends ModalityState> ex = (Class<? extends ModalityState>)Class.forName("com.intellij.openapi.application.impl.ModalityStateEx");
      NON_MODAL = ex.newInstance();
    }
    catch (ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
    catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
    catch (InstantiationException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public abstract String toString();
}
