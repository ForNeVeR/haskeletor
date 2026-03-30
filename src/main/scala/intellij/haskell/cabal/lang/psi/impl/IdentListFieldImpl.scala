// SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
// SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
//
// SPDX-License-Identifier: Apache-2.0

package intellij.haskell.cabal.lang.psi.impl

import com.intellij.psi.PsiElement
import intellij.haskell.cabal.lang.psi.{CabalTypes, IdentList}
import intellij.haskell.psi.HaskellPsiUtil

trait IdentListFieldImpl extends PsiElement {

  /** Retrieves the extension names as strings. */
  def getValue: Array[String] = HaskellPsiUtil.getChildOfType(this, classOf[IdentList]) match {
    case None => Array.empty
    case Some(el) => HaskellPsiUtil.getChildNodes(el, CabalTypes.IDENT).map(_.getText)
  }
}

