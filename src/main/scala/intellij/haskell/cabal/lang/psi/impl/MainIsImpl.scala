// SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
// SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
//
// SPDX-License-Identifier: Apache-2.0

package intellij.haskell.cabal.lang.psi.impl

import com.intellij.psi.PsiElement
import intellij.haskell.cabal.lang.psi.CabalTypes
import intellij.haskell.psi.HaskellPsiUtil

trait MainIsImpl extends PsiElement {

  def getValue: Option[String] = {
    HaskellPsiUtil.getChildNodes(this, CabalTypes.FREEFORM).headOption.map(_.getText)
  }
}
