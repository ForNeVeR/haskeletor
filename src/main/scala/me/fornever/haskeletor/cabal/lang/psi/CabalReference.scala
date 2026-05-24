/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.cabal.lang.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.{PsiElement, PsiNamedElement, PsiReferenceBase}
import me.fornever.haskeletor.cabal.lang.psi.impl.CabalNamedElementImpl

final class CabalReference(el: CabalNamedElementImpl, textRange: TextRange)
  extends PsiReferenceBase[PsiNamedElement](el, textRange):

  override def getVariants: Array[AnyRef] = el.getVariants

  override def resolve(): PsiElement = el.resolve().orNull
