// SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
// SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
//
// SPDX-License-Identifier: Apache-2.0

package intellij.haskell.spellchecker

import com.intellij.psi.PsiElement
import com.intellij.spellchecker.tokenizer.{SpellcheckingStrategy, Tokenizer}
import intellij.haskell.cabal.CabalLanguage

/**
  * Provide spellchecker support for Cabal sources.
  */
class CabalSpellcheckingStrategy extends SpellcheckingStrategy {
  override def isMyContext(element: PsiElement): Boolean = CabalLanguage.Instance.is(element.getLanguage)
}
