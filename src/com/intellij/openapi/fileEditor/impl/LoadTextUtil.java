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
package com.intellij.openapi.fileEditor.impl;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.BinaryFileDecompiler;
import com.intellij.openapi.fileTypes.BinaryFileTypeDecompilers;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Trinity;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.encoding.EncodingManager;
import com.intellij.openapi.vfs.encoding.EncodingRegistry;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

public final class LoadTextUtil {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.fileEditor.impl.LoadTextUtil");
  @Nls private static final String AUTO_DETECTED_FROM_BOM = "auto-detected from BOM";

  private LoadTextUtil() { }

  @NotNull
  private static Pair<CharSequence, String> convertLineSeparators(@NotNull CharBuffer buffer) {
    int dst = 0;
    char prev = ' ';
    int crCount = 0;
    int lfCount = 0;
    int crlfCount = 0;

    final int length = buffer.length();
    final char[] bufferArray = CharArrayUtil.fromSequenceWithoutCopying(buffer);

    for (int src = 0; src < length; src++) {
      char c = bufferArray != null ? bufferArray[src]:buffer.charAt(src);
      switch (c) {
        case '\r':
          if(bufferArray != null) bufferArray[dst++] = '\n';
          else buffer.put(dst++, '\n');
          crCount++;
          break;
        case '\n':
          if (prev == '\r') {
            crCount--;
            crlfCount++;
          }
          else {
            if(bufferArray != null) bufferArray[dst++] = '\n';
            else buffer.put(dst++, '\n');
            lfCount++;
          }
          break;
        default:
          if(bufferArray != null) bufferArray[dst++] = c;
          else buffer.put(dst++, c);
          break;
      }
      prev = c;
    }

    String detectedLineSeparator = null;
    if (crlfCount > crCount && crlfCount > lfCount) {
      detectedLineSeparator = "\r\n";
    }
    else if (crCount > lfCount) {
      detectedLineSeparator = "\r";
    }
    else if (lfCount > 0) {
      detectedLineSeparator = "\n";
    }

    CharSequence result;
    if (buffer.length() == dst) {
      result = buffer;
    }
    else {
      // in Mac JDK CharBuffer.subSequence() signature differs from Oracle's
      // more than that, the signature has changed between jd6 and jdk7,
      // so use more generic CharSequence.subSequence() just in case
      @SuppressWarnings("UnnecessaryLocalVariable") CharSequence seq = buffer;
      result = seq.subSequence(0, dst);
    }
    return Pair.create(result, detectedLineSeparator);
  }

  @NotNull
  private static Charset detectCharset(@NotNull VirtualFile virtualFile, @NotNull byte[] content, @NotNull FileType fileType) {
    Charset charset = null;

    String charsetName = fileType.getCharset(virtualFile, content);
    Trinity<Charset,CharsetToolkit.GuessedEncoding, byte[]> guessed = guessFromContent(virtualFile, content, content.length);

    Charset hardCodedCharset = guessed == null ? null : guessed.first;
    if (charsetName != null) {
      charset = CharsetToolkit.forName(charsetName);
    }
    else if (hardCodedCharset == null) {
      Charset specifiedExplicitly = EncodingRegistry.getInstance().getEncoding(virtualFile, true);
      if (specifiedExplicitly != null) {
        charset = specifiedExplicitly;
      }
    }
    else {
      charset = hardCodedCharset;
    }

    if (charset == null) {
      charset = EncodingRegistry.getInstance().getDefaultCharset();
    }
    virtualFile.setCharset(charset);
    return charset;
  }

  @NotNull
  public static Charset detectCharsetAndSetBOM(@NotNull VirtualFile virtualFile, @NotNull byte[] content) {
    return doDetectCharsetAndSetBOM(virtualFile, content, true).getFirst();
  }

  @NotNull
  private static Pair.NonNull<Charset, byte[]> doDetectCharsetAndSetBOM(@NotNull VirtualFile virtualFile, @NotNull byte[] content, boolean saveBOM) {
    return doDetectCharsetAndSetBOM(virtualFile, content, saveBOM, virtualFile.getFileType());
  }
  @NotNull
  private static Pair.NonNull<Charset, byte[]> doDetectCharsetAndSetBOM(@NotNull VirtualFile virtualFile, @NotNull byte[] content, boolean saveBOM, @NotNull FileType fileType) {
    @NotNull Charset charset = virtualFile.isCharsetSet() ? virtualFile.getCharset() : detectCharset(virtualFile, content,fileType);
    Pair.NonNull<Charset, byte[]> bomAndCharset = getCharsetAndBOM(content, charset);
    final byte[] bom = bomAndCharset.second;
    if (saveBOM && bom.length != 0) {
      virtualFile.setBOM(bom);
      setCharsetWasDetectedFromBytes(virtualFile, AUTO_DETECTED_FROM_BOM);
    }
    return bomAndCharset;
  }

  private static final boolean GUESS_UTF = Boolean.parseBoolean(System.getProperty("idea.guess.utf.encoding", "true"));

  @Nullable("null means no luck, otherwise it's tuple(guessed encoding, hint about content if was unable to guess, BOM)")
  public static Trinity<Charset, CharsetToolkit.GuessedEncoding, byte[]> guessFromContent(@NotNull VirtualFile virtualFile, @NotNull byte[] content, int length) {
    Charset defaultCharset = ObjectUtils.notNull(EncodingManager.getInstance().getEncoding(virtualFile, true), CharsetToolkit.getDefaultSystemCharset());
    CharsetToolkit toolkit = GUESS_UTF ? new CharsetToolkit(content, defaultCharset) : null;
    String detectedFromBytes = null;
    try {
      if (GUESS_UTF) {
        toolkit.setEnforce8Bit(true);
        Charset charset = toolkit.guessFromBOM();
        if (charset != null) {
          detectedFromBytes = AUTO_DETECTED_FROM_BOM;
          byte[] bom = ObjectUtils.notNull(CharsetToolkit.getMandatoryBom(charset), CharsetToolkit.UTF8_BOM);
          return Trinity.create(charset, null, bom);
        }
        CharsetToolkit.GuessedEncoding guessed = toolkit.guessFromContent(length);
        if (guessed == CharsetToolkit.GuessedEncoding.VALID_UTF8) {
          detectedFromBytes = "auto-detected from bytes";
          return Trinity.create(CharsetToolkit.UTF8_CHARSET, guessed, null); //UTF detected, ignore all directives
        }
        if (guessed == CharsetToolkit.GuessedEncoding.SEVEN_BIT) {
          return Trinity.create(null, guessed, null);
        }
      }
      return null;
    }
    finally {
      setCharsetWasDetectedFromBytes(virtualFile, detectedFromBytes);
    }
  }

  @NotNull
  private static Pair.NonNull<Charset,byte[]> getCharsetAndBOM(@NotNull byte[] content, @NotNull Charset charset) {
    if (charset.name().contains(CharsetToolkit.UTF8) && CharsetToolkit.hasUTF8Bom(content)) {
      return Pair.createNonNull(charset, CharsetToolkit.UTF8_BOM);
    }
    try {
      Charset fromBOM = CharsetToolkit.guessFromBOM(content);
      if (fromBOM != null) {
        return Pair.createNonNull(fromBOM, ObjectUtils.notNull(CharsetToolkit.getMandatoryBom(fromBOM), ArrayUtil.EMPTY_BYTE_ARRAY));
      }
    }
    catch (UnsupportedCharsetException ignore) {
    }

    return Pair.createNonNull(charset, ArrayUtil.EMPTY_BYTE_ARRAY);
  }

  /*public static void changeLineSeparators(@Nullable Project project,
                                          @NotNull VirtualFile file,
                                          @NotNull String newSeparator,
                                          @NotNull Object requestor) throws IOException
  {
    CharSequence currentText = getTextByBinaryPresentation(file.contentsToByteArray(), file, true, false);
    String currentSeparator = detectLineSeparator(file, false);
    if (newSeparator.equals(currentSeparator)) {
      return;
    }
    String newText = StringUtil.convertLineSeparators(currentText.toString(), newSeparator);

    file.setDetectedLineSeparator(newSeparator);
    write(project, file, requestor, newText, -1);
  }*/

  /**
   * Overwrites file with text and sets modification stamp and time stamp to the specified values.
   * <p/>
   * Normally you should not use this method.
   *
   * @param requestor            any object to control who called this method. Note that
   *                             it is considered to be an external change if {@code requestor} is {@code null}.
   *                             See { com.intellij.openapi.vfs.VirtualFileEvent#getRequestor}
   * @param newModificationStamp new modification stamp or -1 if no special value should be set @return {@code Writer}
   * @throws IOException if an I/O error occurs
   *  VirtualFile#getModificationStamp()
   */
  public static void write(
                           @NotNull VirtualFile virtualFile,
                           @NotNull Object requestor,
                           @NotNull String text,
                           long newModificationStamp) throws IOException {
/*
    Charset existing = virtualFile.getCharset();
    Pair.NonNull<Charset, byte[]> chosen = charsetForWriting(project, virtualFile, text, existing);
    Charset charset = chosen.first;
    byte[] buffer = chosen.second;
    if (!charset.equals(existing)) {
      virtualFile.setCharset(charset);
    }
    setDetectedFromBytesFlagBack(virtualFile, buffer);

    virtualFile.setBinaryContent(buffer, newModificationStamp, -1, requestor);
*/
  }

  @NotNull
  public static CharSequence loadText(@NotNull final VirtualFile file) {
    if (file instanceof LightVirtualFile) {
      return ((LightVirtualFile)file).getContent();
    }

    if (file.isDirectory()) {
      throw new AssertionError("'" + file.getPresentableUrl() + "' is a directory");
    }

    FileType fileType = file.getFileType();
    if (fileType.isBinary()) {
      final BinaryFileDecompiler decompiler = BinaryFileTypeDecompilers.INSTANCE.forFileType(fileType);
      if (decompiler != null) {
        CharSequence text = decompiler.decompile(file);
        try {
          StringUtil.assertValidSeparators(text);
        }
        catch (AssertionError e) {
          LOG.error(e);
        }
        return text;
      }

      throw new IllegalArgumentException("Attempt to load text for binary file which doesn't have a decompiler plugged in: " +
                                         file.getPresentableUrl() + ". File type: " + fileType.getName());
    }

    try {
      byte[] bytes = file.contentsToByteArray();
      return getTextByBinaryPresentation(bytes, file);
    }
    catch (IOException e) {
      return ArrayUtil.EMPTY_CHAR_SEQUENCE;
    }
  }

  @NotNull
  public static CharSequence getTextByBinaryPresentation(@NotNull final byte[] bytes, @NotNull VirtualFile virtualFile) {
    return getTextByBinaryPresentation(bytes, virtualFile, true, true);
  }

  @NotNull
  public static CharSequence getTextByBinaryPresentation(@NotNull byte[] bytes,
                                                         @NotNull VirtualFile virtualFile,
                                                         boolean saveDetectedSeparators,
                                                         boolean saveBOM) {
    return getTextByBinaryPresentation(bytes, virtualFile, saveDetectedSeparators, saveBOM, virtualFile.getFileType());
  }
  @NotNull
  public static CharSequence getTextByBinaryPresentation(@NotNull byte[] bytes,
                                                         @NotNull VirtualFile virtualFile,
                                                         boolean saveDetectedSeparators,
                                                         boolean saveBOM,
                                                         @NotNull FileType fileType) {
    Pair.NonNull<Charset, byte[]> pair = doDetectCharsetAndSetBOM(virtualFile, bytes, saveBOM, fileType);
    Charset charset = pair.getFirst();
    byte[] bom = pair.getSecond();
    int offset = bom.length;

    Pair<CharSequence, String> result = convertBytes(bytes, charset, offset);
    if (saveDetectedSeparators) {
      virtualFile.setDetectedLineSeparator(result.getSecond());
    }
    return result.getFirst();
  }

  @NotNull
  public static CharSequence getTextByBinaryPresentation(@NotNull byte[] bytes, @NotNull Charset charset) {
    Pair.NonNull<Charset, byte[]> pair = getCharsetAndBOM(bytes, charset);
    byte[] bom = pair.getSecond();
    int offset = bom.length;

    final Pair<CharSequence, String> result = convertBytes(bytes, pair.first, offset);
    return result.getFirst();
  }

  // do not need to think about BOM here. it is processed outside
  @NotNull
  private static Pair<CharSequence, String> convertBytes(@NotNull byte[] bytes, @NotNull Charset charset, final int startOffset) {
    ByteBuffer byteBuffer = ByteBuffer.wrap(bytes, startOffset, bytes.length - startOffset);

    CharBuffer charBuffer;
    try {
      charBuffer = charset.decode(byteBuffer);
    }
    catch (Exception e) {
      // esoteric charsets can throw any kind of exception
      charBuffer = CharBuffer.wrap(ArrayUtil.EMPTY_CHAR_ARRAY);
    }
    return convertLineSeparators(charBuffer);
  }

  private static final Key<String> CHARSET_WAS_DETECTED_FROM_BYTES = Key.create("CHARSET_WAS_DETECTED_FROM_BYTES");

  public static void setCharsetWasDetectedFromBytes(@NotNull VirtualFile virtualFile,
                                                    @Nullable("null if was not detected, otherwise the reason it was") String reason) {
    virtualFile.putUserData(CHARSET_WAS_DETECTED_FROM_BYTES, reason);
  }
}
