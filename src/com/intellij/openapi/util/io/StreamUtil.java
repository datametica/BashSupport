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
package com.intellij.openapi.util.io;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.io.UnsyncByteArrayOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamUtil {
  private static final Logger LOG = Logger.getInstance(StreamUtil.class);

  private StreamUtil() {
  }

  /**
   * Copy stream. Use NetUtils.copyStreamContent(ProgressIndicator, ...) if you want use ProgressIndicator.
   *
   * @param inputStream source stream
   * @param outputStream destination stream
   * @return bytes copied
   */
  public static int copyStreamContent(@NotNull InputStream inputStream, @NotNull OutputStream outputStream) throws IOException {
    final byte[] buffer = new byte[10 * 1024];
    int count;
    int total = 0;
    while ((count = inputStream.read(buffer)) > 0) {
      outputStream.write(buffer, 0, count);
      total += count;
    }
    return total;
  }

  @NotNull
  public static byte[] loadFromStream(@NotNull InputStream inputStream) throws IOException {
    final UnsyncByteArrayOutputStream outputStream = new UnsyncByteArrayOutputStream();
    try {
      copyStreamContent(inputStream, outputStream);
    }
    finally {
      inputStream.close();
    }
    return outputStream.toByteArray();
  }

  /**
   * @deprecated depends on the default encoding, use StreamUtil#readText(java.io.InputStream, String) instead
   */
  @NotNull
  @Deprecated
  public static String readText(@NotNull InputStream inputStream) throws IOException {
    final byte[] data = loadFromStream(inputStream);
    return new String(data);
  }

  @NotNull
  public static String readText(@NotNull InputStream inputStream, @NotNull String encoding) throws IOException {
    final byte[] data = loadFromStream(inputStream);
    return new String(data, encoding);
  }
}
