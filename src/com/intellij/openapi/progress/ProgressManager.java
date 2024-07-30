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
package com.intellij.openapi.progress;

import com.intellij.openapi.components.ServiceManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ProgressManager extends ProgressIndicatorProvider {
  private static ProgressManager ourInstance;

  @NotNull
  @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
  public static ProgressManager getInstance() {
    ProgressManager result = ourInstance;
    if (result == null) {
      result = ServiceManager.getService(ProgressManager.class);
      if (result == null) {
        throw new AssertionError("ProgressManager is null; ");
      }
      else {
        ourInstance = result;
      }
    }
    return result;
  }

  @Override
  public ProgressIndicator getProgressIndicator() {
    return null;
  }

  public static void progress(@NotNull String text) throws ProcessCanceledException {
    progress(text, "");
  }

  public static void progress(@NotNull String text, @Nullable String text2) throws ProcessCanceledException {
    final ProgressIndicator pi = getInstance().getProgressIndicator();
    if (pi != null) {
      pi.checkCanceled();
      pi.setText(text);
      pi.setText2(text2 == null ? "" : text2);
    }
  }

  /**
   * Runs a specified <code>process</code> in a background thread and shows a progress dialog, which can be made non-modal by pressing
   * background button. Upon successful termination of the process a <code>successRunnable</code> will be called in Swing UI thread and
   * <code>canceledRunnable</code> will be called if terminated on behalf of the user by pressing either cancel button, while running in
   * a modal state or stop button if running in background.
   *
   *  project          the project in the context of which the operation is executed.
   * @param progressTitle    the title of the progress window.
   * @param process          the operation to execute.
   * @param successRunnable  a callback to be called in Swing UI thread upon normal termination of the process.
   * @param canceledRunnable a callback to be called in Swing UI thread if the process have been canceled by the user.
   * @deprecated use { #run(Task)}
   */
  public abstract void runProcessWithProgressAsynchronously(
                                                            @NotNull @Nls String progressTitle,
                                                            @NotNull Runnable process,
                                                            @Nullable Runnable successRunnable,
                                                            @Nullable Runnable canceledRunnable);
  /**
   * Runs a specified <code>process</code> in a background thread and shows a progress dialog, which can be made non-modal by pressing
   * background button. Upon successful termination of the process a <code>successRunnable</code> will be called in Swing UI thread and
   * <code>canceledRunnable</code> will be called if terminated on behalf of the user by pressing either cancel button, while running in
   * a modal state or stop button if running in background.
   *
   * project          the project in the context of which the operation is executed.
   * @param progressTitle    the title of the progress window.
   * @param process          the operation to execute.
   * @param successRunnable  a callback to be called in Swing UI thread upon normal termination of the process.
   * @param canceledRunnable a callback to be called in Swing UI thread if the process have been canceled by the user.
   * @param option           progress indicator behavior controller.
   * @deprecated use { #run(Task)}
   */
  public abstract void runProcessWithProgressAsynchronously(
                                                            @NotNull @Nls String progressTitle,
                                                            @NotNull Runnable process,
                                                            @Nullable Runnable successRunnable,
                                                            @Nullable Runnable canceledRunnable,
                                                            @NotNull PerformInBackgroundOption option);

  /**
   * Runs a specified <code>task</code> in either background/foreground thread and shows a progress dialog.
   *
   * @param task task to run (either { Task.Modal} or { Task.Backgroundable}).
   */
  public abstract void run(@NotNull Task task);

  /**
   * Runs a specified computation with a modal progress dialog.
   */
  public <T, E extends Exception> T run(@NotNull Task.WithResult<T, E> task) throws E {
    run((Task)task);
    return task.getResult();
  }

  @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
  public static void checkCanceled() throws ProcessCanceledException {
    getInstance().doCheckCanceled();
  }

}