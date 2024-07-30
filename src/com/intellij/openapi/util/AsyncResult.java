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
package com.intellij.openapi.util;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AsyncResult<T> extends ActionCallback {
  private static final Logger LOG = Logger.getInstance(AsyncResult.class);

  protected T myResult;

  public AsyncResult() {
  }

  AsyncResult(int countToDone, @Nullable T result) {
    super(countToDone);

    myResult = result;
  }

  @NotNull
  public AsyncResult<T> setDone(T result) {
    myResult = result;
    setDone();
    return this;
  }

  @NotNull
  public AsyncResult<T> setRejected(T result) {
    myResult = result;
    setRejected();
    return this;
  }

  /**
   * @deprecated Use { #doWhenDone(com.intellij.util.Consumer)} (to remove in IDEA 16)
   */
  @NotNull
  @Deprecated
  public AsyncResult<T> doWhenDone(@SuppressWarnings("deprecation") @NotNull final Handler<T> handler) {
    doWhenDone(new Runnable() {
      @Override
      public void run() {
        handler.run(myResult);
      }
    });
    return this;
  }

  @Override
  @NotNull
  public final AsyncResult<T> notify(@NotNull final ActionCallback child) {
    super.notify(child);
    return this;
  }

  public T getResult() {
    return myResult;
  }

  /**
   * @deprecated Use { com.intellij.util.Consumer} (to remove in IDEA 16)
   */
  @Deprecated
  public interface Handler<T> {
    void run(T t);
  }

  /**
   * @deprecated Don't use AsyncResult - use Promise instead.
   */
  @Deprecated
  public static class Done<T> extends AsyncResult<T> {
    public Done(T value) {
      setDone(value);
    }
  }

  /**
   * @deprecated Don't use AsyncResult - use Promise instead.
   */
  @Deprecated
  public static class Rejected<T> extends AsyncResult<T> {
    public Rejected() {
      setRejected();
    }

    public Rejected(T value) {
      setRejected(value);
    }
  }

  /**
   * @deprecated Don't use AsyncResult - use Promise instead.
   */
  @NotNull
  @Deprecated
  public static <R> AsyncResult<R> rejected() {
    //noinspection unchecked,deprecation
    return new Rejected();
  }

  /**
   * @deprecated Don't use AsyncResult - use Promise instead.
   */
  @NotNull
  @Deprecated
  public static <R> AsyncResult<R> rejected(@NotNull String errorMessage) {
    AsyncResult<R> result = new AsyncResult<R>();
    result.reject(errorMessage);
    return result;
  }

  @NotNull
  public static <R> AsyncResult<R> done(@Nullable R result) {
    return new AsyncResult<R>().setDone(result);
  }
}
