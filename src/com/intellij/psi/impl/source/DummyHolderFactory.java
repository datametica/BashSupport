/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * @author max
 */
package com.intellij.psi.impl.source;

import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.util.CharTable;

public class DummyHolderFactory  {
  private static HolderFactory INSTANCE = new DefaultFactory();

  private DummyHolderFactory() {}

  public static void setFactory(HolderFactory factory) {
    INSTANCE = factory;
  }

  public static DummyHolder createHolder(TreeElement contentElement, PsiElement context) {
    return INSTANCE.createHolder( contentElement, context);
  }

  public static DummyHolder createHolder(CharTable table, boolean validity) {
    return INSTANCE.createHolder( table, validity);
  }

  public static DummyHolder createHolder(PsiElement context) {
    return INSTANCE.createHolder( context);
  }

  public static DummyHolder createHolder(Language language, PsiElement context) {
    return INSTANCE.createHolder( language, context);
  }

  public static DummyHolder createHolder(TreeElement contentElement, PsiElement context, CharTable table) {
    return INSTANCE.createHolder( contentElement, context, table);
  }

  public static DummyHolder createHolder(PsiElement context, CharTable table) {
    return INSTANCE.createHolder( context, table);
  }

  public static DummyHolder createHolder(final CharTable table, final Language language) {
    return INSTANCE.createHolder( table, language);
  }

  private static class DefaultFactory implements HolderFactory {
    @Override
    public DummyHolder createHolder(TreeElement contentElement, PsiElement context) {
      return new DummyHolder( contentElement, context);
    }

    @Override
    public DummyHolder createHolder(CharTable table, boolean validity) {
      return new DummyHolder( table, validity);
    }

    @Override
    public DummyHolder createHolder(PsiElement context) {
      return new DummyHolder( context);
    }

    @Override
    public DummyHolder createHolder(final Language language, final PsiElement context) {
      return new DummyHolder( language, context);
    }

    @Override
    public DummyHolder createHolder(TreeElement contentElement, PsiElement context, CharTable table) {
      return new DummyHolder( contentElement, context, table);
    }

    @Override
    public DummyHolder createHolder(PsiElement context, CharTable table) {
      return new DummyHolder( context, table);
    }

    @Override
    public DummyHolder createHolder(final CharTable table, final Language language) {
      return new DummyHolder( table, language);
    }
  }
}