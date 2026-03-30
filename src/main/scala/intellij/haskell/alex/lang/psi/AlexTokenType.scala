// SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
// SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
//
// SPDX-License-Identifier: Apache-2.0

package intellij.haskell.alex.lang.psi

import com.intellij.psi.tree.IElementType
import intellij.haskell.alex.AlexLanguage

/**
  * @author ice1000
  */
class AlexTokenType(debugName: String) extends IElementType(debugName, AlexLanguage.Instance) {
}
