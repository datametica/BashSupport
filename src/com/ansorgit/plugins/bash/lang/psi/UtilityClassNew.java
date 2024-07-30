package com.ansorgit.plugins.bash.lang.psi;

import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.psi.PsiElement;

public class UtilityClassNew {
    public boolean areElementsEquivalent(PsiElement element1, PsiElement element2) {
        ProgressIndicatorProvider.checkCanceled();
        if (element1 == element2) {
            return true;
        } else if (element1 != null && element2 != null) {
            return element1.equals(element2) || element1.isEquivalentTo(element2) || element2.isEquivalentTo(element1);
        } else {
            return false;
        }
    }

}
