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
package com.intellij.util.concurrency;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.Consumer;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A ThreadPoolExecutor which also implements { ScheduledExecutorService} by awaiting scheduled tasks in a separate thread
 * and then executing them in the owned ThreadPoolExecutor.
 * Unlike the existing { ScheduledThreadPoolExecutor}, this pool is unbounded.
 */
public class AppScheduledExecutorService extends SchedulingWrapper {
  private static final Logger LOG = Logger.getInstance("#org.jetbrains.ide.PooledThreadExecutor");
  static final String POOLED_THREAD_PREFIX = "ApplicationImpl pooled thread ";
  @NotNull private final String myName;
  private Consumer<Thread> newThreadListener;
  private final AtomicInteger counter = new AtomicInteger();

  private static class Holder {
    private static final AppScheduledExecutorService INSTANCE = new AppScheduledExecutorService("Global instance");
  }

  @NotNull
  static ScheduledExecutorService getInstance() {
    return Holder.INSTANCE;
  }

  AppScheduledExecutorService(@NotNull final String name) {
    super(new BackendThreadPoolExecutor(), new AppDelayQueue());
    myName = name;
    ((BackendThreadPoolExecutor)backendExecutorService).doSetThreadFactory(new ThreadFactory() {
      @NotNull
      @Override
      public Thread newThread(@NotNull final Runnable r) {
        Thread thread = new Thread(r, POOLED_THREAD_PREFIX + counter.incrementAndGet());

        thread.setPriority(Thread.NORM_PRIORITY - 1);

        Consumer<Thread> listener = newThreadListener;
        if (listener != null) {
          listener.consume(thread);
        }
        return thread;
      }
    });
  }

  @NotNull
  @Override
  public List<Runnable> shutdownNow() {
    return error();
  }

  @Override
  public void shutdown() {
    error();
  }

  static List<Runnable> error() {
    throw new IncorrectOperationException("You must not call this method on the global app pool");
  }

  @Override
  void doShutdown() {
    super.doShutdown();
    ((BackendThreadPoolExecutor)backendExecutorService).doShutdown();
  }

  @NotNull
  @Override
  List<Runnable> doShutdownNow() {
    return ContainerUtil.concat(super.doShutdownNow(), ((BackendThreadPoolExecutor)backendExecutorService).doShutdownNow());
  }

  private static class BackendThreadPoolExecutor extends ThreadPoolExecutor {
    BackendThreadPoolExecutor() {
      super(1, Integer.MAX_VALUE, 1, TimeUnit.MINUTES, new SynchronousQueue<Runnable>());
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("beforeExecute " + BoundedTaskExecutor.info(r) + " in " + t);
      }
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("afterExecute  " + BoundedTaskExecutor.info(r) + " in " + Thread.currentThread());
      }

      if (t != null) {
        LOG.error("Worker exited due to exception", t);
      }
    }

    private void doShutdown() {
      super.shutdown();
    }

    @NotNull
    private List<Runnable> doShutdownNow() {
      return super.shutdownNow();
    }

    private void doSetThreadFactory(@NotNull ThreadFactory threadFactory) {
      super.setThreadFactory(threadFactory);
    }

    // stub out sensitive methods
    @Override
    public void shutdown() {
      error();
    }

    @NotNull
    @Override
    public List<Runnable> shutdownNow() {
      return error();
    }

    @Override
    public void setCorePoolSize(int corePoolSize) {
      error();
    }

    @Override
    public void allowCoreThreadTimeOut(boolean value) {
      error();
    }

    @Override
    public void setMaximumPoolSize(int maximumPoolSize) {
      error();
    }

    @Override
    public void setKeepAliveTime(long time, TimeUnit unit) {
      error();
    }

    @Override
    public void setThreadFactory(ThreadFactory threadFactory) {
      error();
    }
  }
}
