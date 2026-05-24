/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.cabal.lang.psi.impl

import com.intellij.psi.{PsiElement, PsiReference}
import me.fornever.haskeletor.cabal.lang.psi.{CabalNamedElement, CabalReference}

trait CabalNamedElementImpl extends CabalNamedElement:

  def getVariants: Array[AnyRef]

  def resolve(): Option[PsiElement]

  override def getReference: PsiReference =
    new CabalReference(this, getTextRange)
