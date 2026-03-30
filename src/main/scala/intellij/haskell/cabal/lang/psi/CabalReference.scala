// SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
// SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
//
// SPDX-License-Identifier: Apache-2.0

package intellij.haskell.cabal.lang.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.{PsiElement, PsiNamedElement, PsiReferenceBase}

import intellij.haskell.cabal.lang.psi.impl.CabalNamedElementImpl

final class CabalReference(el: CabalNamedElementImpl, textRange: TextRange)
  extends PsiReferenceBase[PsiNamedElement](el, textRange) {

  override def getVariants: Array[AnyRef] = el.getVariants

  override def resolve(): PsiElement = el.resolve().orNull
}
