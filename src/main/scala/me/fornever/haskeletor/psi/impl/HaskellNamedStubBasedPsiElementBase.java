/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.psi.impl;

import com.intellij.extapi.psi.StubBasedPsiElementBase;
import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import me.fornever.haskeletor.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import scala.Option;

/**
 * Base class for named stub-based PSI elements (varid, conid, varsym, consym, modid).
 * Provides runtime implementations of methods previously generated via psiImplUtilClass.
 */
public abstract class HaskellNamedStubBasedPsiElementBase<T extends StubElement<?>> extends StubBasedPsiElementBase<T> implements HaskellNamedElement {

    HaskellNamedStubBasedPsiElementBase(@NotNull T stub, IStubElementType nodeType) {
        super(stub, nodeType);
    }

    HaskellNamedStubBasedPsiElementBase(ASTNode node) {
        super(node);
    }

    @Override
    public String getName() {
        return getText();
    }

    @Nullable
    @Override
    public PsiElement getNameIdentifier() {
        return this;
    }

    @Override
    public PsiElement setName(@NotNull String name) {
        if (this instanceof HaskellVarid) {
            Option<HaskellVarid> newElement = HaskellElementFactory.createVarid(getProject(), name);
            if (newElement.isDefined()) replace(newElement.get());
        } else if (this instanceof HaskellConid) {
            Option<HaskellConid> newElement = HaskellElementFactory.createConid(getProject(), name);
            if (newElement.isDefined()) replace(newElement.get());
        } else if (this instanceof HaskellVarsym) {
            Option<HaskellVarsym> newElement = HaskellElementFactory.createVarsym(getProject(), name);
            if (newElement.isDefined()) replace(newElement.get());
        } else if (this instanceof HaskellConsym) {
            Option<HaskellConsym> newElement = HaskellElementFactory.createConsym(getProject(), name);
            if (newElement.isDefined()) replace(newElement.get());
        } else if (this instanceof HaskellModid) {
            Option<HaskellModid> newElement = HaskellElementFactory.createModid(getProject(), name);
            if (newElement.isDefined()) replace(newElement.get());
        }
        return this;
    }

    @Override
    public @Nullable PsiReference getReference() {
        return HaskellPsiImplUtil.getReference(this);
    }

    @Override
    public ItemPresentation getPresentation() {
        return HaskellPsiImplUtil.getPresentation(this);
    }
}
