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

public interface HolderFactory {
  DummyHolder createHolder(TreeElement contentElement, PsiElement context);
  DummyHolder createHolder(CharTable table, boolean validity);
  DummyHolder createHolder(PsiElement context);
  DummyHolder createHolder(Language language, PsiElement context);
  DummyHolder createHolder(TreeElement contentElement, PsiElement context, CharTable table);
  DummyHolder createHolder(PsiElement context, CharTable table);
  DummyHolder createHolder(final CharTable table, final Language language);
  
}