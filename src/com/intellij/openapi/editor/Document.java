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
package com.intellij.openapi.editor;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.UserDataHolder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the contents of a text file loaded into memory, and possibly opened in an IDEA
 * text editor. Line breaks in the document text are always normalized as single \n characters,
 * and are converted to proper format when the document is saved.
 * <p/>
 * Please see <a href="http://confluence.jetbrains.net/display/IDEADEV/IntelliJ+IDEA+Architectural+Overview">IntelliJ IDEA Architectural Overview </a>
 * for high-level overview.
 *
 *  Editor#getDocument()
 *  com.intellij.psi.PsiDocumentManager
 *  com.intellij.openapi.fileEditor.FileDocumentManager
 *  EditorFactory#createDocument(CharSequence)
 */
public interface Document extends UserDataHolder {
  Document[] EMPTY_ARRAY = new Document[0];

  /**
   * Retrieves a copy of the document content. For obvious performance reasons use
   * { #getCharsSequence()} whenever it's possible.
   *
   * @return document content.
   */
  @NotNull
  @Contract(pure=true)
  String getText();

  @NotNull
  @Contract(pure=true)
  String getText(@NotNull TextRange range);

  /**
   * Use this method instead of { #getText()} if you do not need to create a copy of the content.
   * Content represented by returned CharSequence is subject to change whenever document is modified via delete/replace/insertString method
   * calls. It is necessary to obtain Application.runWriteAction() to modify content of the document though so threading issues won't
   * arise.
   *
   * @return inplace document content.
   *  #getTextLength()
   */
  @Contract(pure=true)
  @NotNull
  CharSequence getCharsSequence();

  /**
   * @deprecated Use { #getCharsSequence()} or { #getText()} instead.
   */
  @Deprecated
  @NotNull char[] getChars();

  /**
   * Returns the length of the document text.
   *
   * @return the length of the document text.
   *  #getCharsSequence()
   */
  @Contract(pure=true)
  int getTextLength();

  /**
   * Returns the line number (0-based) corresponding to the specified offset in the document.
   *
   * @param offset the offset to get the line number for (must be in the range from 0 to
   * getTextLength()-1)
   * @return the line number corresponding to the offset.
   */
  @Contract(pure=true)
  int getLineNumber(int offset);

  /**
   * Returns the start offset for the line with the specified number.
   *
   * @param line the line number (from 0 to getLineCount()-1)
   * @return the start offset for the line.
   */
  @Contract(pure=true)
  int getLineStartOffset(int line);

  /**
   * Returns the end offset for the line with the specified number.
   *
   * @param line the line number (from 0 to getLineCount()-1)
   * @return the end offset for the line.
   */
  @Contract(pure=true)
  int getLineEndOffset(int line);

  /**
   * Replaces the specified range of text in the document with the specified string.
   * Line breaks in the text to replace with must be normalized as \n.
   *
   * @param startOffset the start offset of the range to replace.
   * @param endOffset the end offset of the range to replace.
   * @param s the text to replace with.
   * @throws ReadOnlyModificationException if the document is read-only.
   * @throws ReadOnlyFragmentModificationException if the fragment to be modified is covered by a guarded block.
   */
  void replaceString(int startOffset, int endOffset, @NotNull CharSequence s);

  /**
   * Checks if the document text is read-only.
   *
   * @return true if the document text is writable, false if it is read-only.
   *  #fireReadOnlyModificationAttempt()
   */
  @Contract(pure=true)
  boolean isWritable();

  /**
   * Gets the modification stamp value. Modification stamp is a value changed by any modification
   * of the content of the file. Note that it is not related to the file modification time.
   *
   * @return the modification stamp value.
   *  com.intellij.psi.PsiFile#getModificationStamp()
   *  com.intellij.openapi.vfs.VirtualFile#getModificationStamp()
   */
  @Contract(pure=true)
  long getModificationStamp();

  void setText(@NotNull final CharSequence text);
}
