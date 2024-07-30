package com.intellij.lang.impl;

import com.intellij.lang.*;
import com.intellij.openapi.util.Key;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.diff.FlyweightCapableTreeStructure;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PsiBuilderAdapter implements PsiBuilder {
    protected final PsiBuilder myDelegate;

    public PsiBuilderAdapter(PsiBuilder delegate) {
        this.myDelegate = delegate;
    }

    public PsiBuilder getDelegate() {
        return this.myDelegate;
    }

    /*public Project getProject() {
        return this.myDelegate.getProject();
    }*/

    public CharSequence getOriginalText() {
        return this.myDelegate.getOriginalText();
    }

    public void advanceLexer() {
        this.myDelegate.advanceLexer();
    }

    @Nullable
    public IElementType getTokenType() {
        return this.myDelegate.getTokenType();
    }

    public void setTokenTypeRemapper(ITokenTypeRemapper remapper) {
        this.myDelegate.setTokenTypeRemapper(remapper);
    }

    public void setWhitespaceSkippedCallback(@Nullable WhitespaceSkippedCallback callback) {
        this.myDelegate.setWhitespaceSkippedCallback(callback);
    }

    public void remapCurrentToken(IElementType type) {
        this.myDelegate.remapCurrentToken(type);
    }

    public IElementType lookAhead(int steps) {
        return this.myDelegate.lookAhead(steps);
    }

    public IElementType rawLookup(int steps) {
        return this.myDelegate.rawLookup(steps);
    }

    public int rawTokenTypeStart(int steps) {
        return this.myDelegate.rawTokenTypeStart(steps);
    }

    public int rawTokenIndex() {
        return this.myDelegate.rawTokenIndex();
    }

    @Nullable
    @NonNls
    public String getTokenText() {
        return this.myDelegate.getTokenText();
    }

    public int getCurrentOffset() {
        return this.myDelegate.getCurrentOffset();
    }

    @NotNull
    public PsiBuilder.Marker mark() {
        return this.myDelegate.mark();
    }

    public void error(String messageText) {
        this.myDelegate.error(messageText);
    }

    public boolean eof() {
        return this.myDelegate.eof();
    }

    @NotNull
    public ASTNode getTreeBuilt() {
        return this.myDelegate.getTreeBuilt();
    }

    @NotNull
    public FlyweightCapableTreeStructure<LighterASTNode> getLightTree() {
        return this.myDelegate.getLightTree();
    }

    public void setDebugMode(boolean dbgMode) {
        this.myDelegate.setDebugMode(dbgMode);
    }

    public void enforceCommentTokens(@NotNull TokenSet tokens) {
        this.myDelegate.enforceCommentTokens(tokens);
    }

    @Nullable
    public LighterASTNode getLatestDoneMarker() {
        return this.myDelegate.getLatestDoneMarker();
    }

    @Nullable
    public <T> T getUserData(@NotNull Key<T> key) {
        return this.myDelegate.getUserData(key);
    }

    public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
        this.myDelegate.putUserData(key, value);
    }

    public <T> T getUserDataUnprotected(@NotNull Key<T> key) {
        return this.myDelegate.getUserDataUnprotected(key);
    }

    public <T> void putUserDataUnprotected(@NotNull Key<T> key, @Nullable T value) {
        this.myDelegate.putUserDataUnprotected(key, value);
    }

}
