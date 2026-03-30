// SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
// SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
//
// SPDX-License-Identifier: Apache-2.0

package intellij.haskell.cabal.lang.psi.impl

import com.intellij.psi.PsiElement
import intellij.haskell.cabal.lang.psi.ModulePart

trait ModuleImpl extends PsiElement {

  def getParts: Array[ModulePart] = getChildren.map(assertModulePart)

  def getFirstPart: ModulePart = assertModulePart(getFirstChild)

  def getLastPart: ModulePart = assertModulePart(getLastChild)

  def getModuleName: String = {
    this.getText
  }

  private def assertModulePart(el: PsiElement): ModulePart = el match {
    case el: ModulePart => el
    case other => throw new CabalElementTypeError("ModulePart", other)
  }
}
