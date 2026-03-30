// SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
// SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
//
// SPDX-License-Identifier: Apache-2.0

package intellij.haskell.alex.lang.lexer

import com.intellij.lexer.FlexAdapter

/**
  * @author ice1000
  */
class AlexLexer extends FlexAdapter(new _AlexLexer) {
}
