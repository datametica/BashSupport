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

import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Allows to { #schedule(Callable, long, TimeUnit)} tasks later
 * and execute them in parallel in the {@code backendExecutor} with not more than at {@code maxSimultaneousTasks} at a time.
 */
class BoundedScheduledExecutorService extends SchedulingWrapper {
  BoundedScheduledExecutorService(@NotNull String name, @NotNull ExecutorService backendExecutor, int maxSimultaneousTasks) {
    super(new BoundedTaskExecutor(name, backendExecutor, maxSimultaneousTasks),
          ((AppScheduledExecutorService)AppExecutorUtil.getAppScheduledExecutorService()).delayQueue);
    assert !(backendExecutor instanceof ScheduledExecutorService) : "backendExecutor is already ScheduledExecutorService: " + backendExecutor;
  }

  @Override
  public void shutdown() {
    super.shutdown();
    backendExecutorService.shutdown();
  }

  @NotNull
  @Override
  public List<Runnable> shutdownNow() {
    return ContainerUtil.concat(super.shutdownNow(), backendExecutorService.shutdownNow());
  }

  @Override
  public boolean isShutdown() {
    return super.isShutdown() && backendExecutorService.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return super.isTerminated() && backendExecutorService.isTerminated();
  }
}
