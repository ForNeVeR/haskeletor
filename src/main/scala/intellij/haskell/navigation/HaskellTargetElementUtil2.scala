// SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
// SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
//
// SPDX-License-Identifier: Apache-2.0

package intellij.haskell.navigation

import com.intellij.codeInsight.TargetElementEvaluatorEx2
import com.intellij.psi.PsiElement
import intellij.haskell.psi.HaskellNamedElement

class HaskellTargetElementUtil2 extends TargetElementEvaluatorEx2 {

  override def getNamedElement(element: PsiElement): PsiElement = {
    if (element.isInstanceOf[HaskellNamedElement]) {
      element
    } else {
      null
    }
  }

  override def isAcceptableNamedParent(parent: PsiElement): Boolean = {
    false
  }
}