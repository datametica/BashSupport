// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.psi.util

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType


val PsiElement?.elementType: IElementType?
    get() = PsiUtilCore.getElementType(this)

