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
package com.intellij.lang;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Interface for defining custom element's edge processors for {plain com.intellij.lang.PsiBuilder PSI builder}.
 * Each element has a pair of edge processors: for it's left and right edge. Edge processors defines position
 * of element start and end in token stream with recognition of whitespace and comment tokens surrounding the element.
 *
 *  com.intellij.lang.PsiBuilder.Marker#setCustomEdgeTokenBinders(WhitespacesAndCommentsBinder, WhitespacesAndCommentsBinder)
 */
public interface WhitespacesAndCommentsBinder {
  /**
   * Provides an ability for the processor to get a text of any of given tokens.
   */
  interface TokenTextGetter {
    @NotNull
    CharSequence get(int i);
  }

  /**
   * Recursive binder is allowed to adjust nested elements positions.
   */
  interface RecursiveBinder extends WhitespacesAndCommentsBinder {

  }

  /**
   * <p>Analyzes whitespace and comment tokens at element's edge and returns element's edge position relative to these tokens.
   * Value returned by left edge processor will be used as a pointer to a first token of element.
   * Value returned by right edge processor will be used as a pointer to a token next of element's last token.
   * <p/>
   * <p>Example 1: if a processor for left edge wants to leave all whitespaces and comments out of element's scope
   * (before it's start) it should return value of <code>tokens.size()</code> placing element's start pointer to a first
   * token after series of whitespaces/comments.
   * <p/>
   * <p>Example 2: if a processor for right edge wants to leave all whitespaces and comments out of element's scope
   * (after it's end) it should return value of <code>0</code> placing element's end pointer to a first
   * whitespace or comment after element's end.
   *
   * @param tokens sequence of whitespace and comment tokens at the element's edge.
   * @param atStreamEdge true if sequence of tokens is located at the beginning or the end of token stream.
   * @param getter token text getter.
   * @return position of element's edge relative to given tokens.
   */
  int getEdgePosition(List<IElementType> tokens, boolean atStreamEdge, TokenTextGetter getter);
}
