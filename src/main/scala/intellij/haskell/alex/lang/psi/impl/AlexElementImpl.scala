// SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
// SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
//
// SPDX-License-Identifier: Apache-2.0

package intellij.haskell.alex.lang.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import intellij.haskell.alex.lang.psi.AlexElement

/**
  * @author ice1000
  */
class AlexElementImpl private[impl](node: ASTNode) extends ASTWrapperPsiElement(node) with AlexElement {
}
