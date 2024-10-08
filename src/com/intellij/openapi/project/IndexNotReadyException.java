/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.project;

import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.ExceptionWithAttachments;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Thrown on accessing indices when they're not ready, in so-called dumb mode. Possible fixes:
 * <ul>
 * <li> If { com.intellij.openapi.actionSystem.AnAction#actionPerformed(com.intellij.openapi.actionSystem.AnActionEvent)} is in stack trace,
 * consider making the action not implement { DumbAware}.
 *
 * <li> A { DumbAware} action, having got this exception, may just notify the user that the requested activity is not possible while
 * indexing is in progress. It can be done via a dialog (see { com.intellij.openapi.ui.Messages}) or a status bar balloon
 * (see { DumbService#showDumbModeNotification(String)}, { com.intellij.openapi.actionSystem.ex.ActionUtil#showDumbModeWarning(com.intellij.openapi.actionSystem.AnActionEvent...)}).
 *
 * <li> If index access is performed from some non-urgent invokeLater activity, consider replacing it with
 * { DumbService#smartInvokeLater(Runnable)}. Note that this 'later' can be very late, several minutes may pass. So if that code
 * involves user interaction, { DumbService#smartInvokeLater(Runnable)} should probably not be used to avoid dialogs popping out of the blue.
 *
 * <li> If it's a non-urgent background process (e.g. compilation, usage search), consider replacing topmost read-action with
 * { DumbService#runReadActionInSmartMode(Computable)}.
 *
 * <li> If the exception comes from within Java's findClass call, and the IDE is currently performing a user-initiated action or a
 * task when skipping findClass would lead to very negative consequences (e.g. not stopping at a breakpoint), then it might be possible
 * to avoid index query by using alternative resolve (and findClass) strategy, which is significantly slower and might return null. To do this,
 * use { DumbService#setAlternativeResolveEnabled(boolean)}.
 *
 * <li> It's preferable to avoid the exception entirely by adding { DumbService#isDumb()} checks where necessary.
 * </ul>
 *
 * @author peter
 *  DumbService
 *  DumbAware
 */
public class IndexNotReadyException extends RuntimeException implements ExceptionWithAttachments {
  @Nullable private final Throwable myStartTrace;

  public IndexNotReadyException() {
    this(null);
  }

  public IndexNotReadyException(@Nullable Throwable startTrace) {
    super("Please change caller according to " + IndexNotReadyException.class.getName() + " documentation");
    myStartTrace = startTrace;
  }

  @NotNull
  @Override
  public Attachment[] getAttachments() {
    return myStartTrace == null
           ? Attachment.EMPTY_ARRAY
           : new Attachment[]{new Attachment("indexingStart", myStartTrace)};
  }
}
