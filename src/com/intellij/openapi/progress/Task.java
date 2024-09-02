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
package com.intellij.openapi.progress;

import com.intellij.CommonBundle;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Ref;
import com.intellij.util.ExceptionUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * Intended to run tasks, both modal and non-modal (backgroundable)
 * Example of use:
 * <pre>
 * new Task.Backgroundable(project, "Synchronizing data", true) {
 *  public void run(ProgressIndicator indicator) {
 *    indicator.setText("Loading changes");
 *    indicator.setFraction(0.0);
 *    // some code
 *    indicator.setFraction(1.0);
 *  }
 * }.setCancelText("Stop loading").queue();
 * </pre>
 *
 *  ProgressManager#run(Task)
 */
public abstract class Task implements TaskInfo, Progressive {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.progress.Task");

  protected String myTitle;
  private final boolean myCanBeCancelled;

  private String myCancelText = CommonBundle.getCancelButtonText();
  private String myCancelTooltipText = CommonBundle.getCancelButtonText();

  public Task(@Nls(capitalization = Nls.Capitalization.Title) @NotNull String title, boolean canBeCancelled) {
    myTitle = title;
    myCanBeCancelled = canBeCancelled;
  }

  public final void queue() {
    ProgressManager.getInstance().run(this);
  }

  public abstract static class Modal extends Task {
    public Modal(@Nls(capitalization = Nls.Capitalization.Title) @NotNull String title, boolean canBeCancelled) {
      super(title, canBeCancelled);
    }
  }

  public abstract static class WithResult<T, E extends Exception> extends Modal {
    private final Ref<T> myResult = Ref.create();
    private final Ref<Throwable> myError = Ref.create();

    public WithResult( @Nls(capitalization = Nls.Capitalization.Title) @NotNull String title, boolean canBeCancelled) {
      super( title, canBeCancelled);
    }

    @Override
    public final void run(@NotNull ProgressIndicator indicator) {
      try {
        myResult.set(compute(indicator));
      }
      catch (Throwable t) {
        myError.set(t);
      }
    }

    protected abstract T compute(@NotNull ProgressIndicator indicator) throws E;

    @SuppressWarnings("unchecked")
    public T getResult() throws E {
      Throwable t = myError.get();
      ExceptionUtil.rethrowUnchecked(t);
      if (t != null) throw (E)t;
      return myResult.get();
    }
  }
}