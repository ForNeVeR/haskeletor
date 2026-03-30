// SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
// SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
//
// SPDX-License-Identifier: Apache-2.0

package intellij.haskell.cabal.lang.psi

import com.intellij.psi.PsiElement
import intellij.haskell.cabal.lang.psi
import intellij.haskell.psi.HaskellPsiUtil

object CabalPsiUtil {

  def getFieldContext(el: PsiElement): Option[psi.CabalFieldElement] = {
    HaskellPsiUtil.collectFirstParent(el) { case el: psi.CabalFieldElement => el }
  }
}
