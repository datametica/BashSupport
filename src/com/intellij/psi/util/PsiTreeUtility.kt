package com.intellij.psi.util

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType


val PsiElement?.elementType: IElementType?
    get() = PsiUtilCore.getElementType(this)

