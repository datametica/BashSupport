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
package com.intellij.execution.filters;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Yura Cangea
 * @version 1.0
 */
public interface Filter {

  Filter[] EMPTY_ARRAY = new Filter[0];

  class Result extends ResultItem {

    protected NextAction myNextAction = NextAction.EXIT;
    protected final List<ResultItem> myResultItems;

    public Result(final int highlightStartOffset, final int highlightEndOffset, @Nullable final HyperlinkInfo hyperlinkInfo) {
      this(highlightStartOffset, highlightEndOffset, hyperlinkInfo, false);
    }

    public Result(final int highlightStartOffset,
                  final int highlightEndOffset,
                  @Nullable final HyperlinkInfo hyperlinkInfo,
                  final boolean grayedHyperlink) {
      //super(highlightStartOffset, highlightEndOffset, hyperlinkInfo);
      myResultItems = null;
    }

    public Result(@NotNull List<ResultItem> resultItems) {
      //super(-1, -1, null, null, null);
      myResultItems = resultItems;
    }


    /**
     * @deprecated This method will be removed. Result may be constructed using ResultItems, in that case this method will return incorrect value. Use { #getResultItems()} instead.
     */
    @Deprecated
    @Override
    public int getHighlightStartOffset() {
      return super.getHighlightStartOffset();
    }

    /**
     * @deprecated This method will be removed. Result may be constructed using ResultItems, in that case this method will return incorrect value. Use { #getResultItems()} instead.
     */
    @Deprecated
    @Override
    public int getHighlightEndOffset() {
      return super.getHighlightEndOffset();
    }

    /**
     * @deprecated This method will be removed. Result may be constructed using ResultItems, in that case this method will return incorrect value. Use { #getResultItems()} or { #getFirstHyperlinkInfo()} instead.
     */
    @Deprecated
    @Nullable
    @Override
    public HyperlinkInfo getHyperlinkInfo() {
      return super.getHyperlinkInfo();
    }
  }

  enum NextAction {
    EXIT, CONTINUE_FILTERING,
  }

  class ResultItem {
    /**
     * @deprecated use getter, the visibility of this field will be decreased.
     */
    @Deprecated
    public final int highlightStartOffset = 1;
    /**
     * @deprecated use getter, the visibility of this field will be decreased.
     */
    @Deprecated
    public final int highlightEndOffset = 1;
    /**
     * @deprecated use getter, the visibility of this field will be decreased.
     */
    /*@Deprecated @Nullable
    public final TextAttributes highlightAttributes;*/
    /**
     * @deprecated use getter, the visibility of this field will be decreased.
     */
    @Deprecated @Nullable
    public final HyperlinkInfo hyperlinkInfo = null;

    //private final TextAttributes myFollowedHyperlinkAttributes;

    public int getHighlightStartOffset() {
      //noinspection deprecation
      return highlightStartOffset;
    }

    public int getHighlightEndOffset() {
      //noinspection deprecation
      return highlightEndOffset;
    }

    @Nullable
    public HyperlinkInfo getHyperlinkInfo() {
      //noinspection deprecation
      return hyperlinkInfo;
    }
  }
}
