// SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
// SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
//
// SPDX-License-Identifier: Apache-2.0

package intellij.haskell.spellchecker

import intellij.haskell.HaskellLanguage
import com.intellij.psi.PsiElement
import com.intellij.spellchecker.tokenizer.{Tokenizer, SpellcheckingStrategy}

/**
 * Provide spellchecker support for Haskell sources.
 */
class HaskellSpellcheckingStrategy extends SpellcheckingStrategy {
  override def isMyContext(element: PsiElement): Boolean = HaskellLanguage.Instance.is(element.getLanguage)
}
