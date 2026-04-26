/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.spellchecker

import com.intellij.psi.PsiElement
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy
import me.fornever.haskeletor.core.cabal.CabalLanguage

/**
  * Provide spellchecker support for Cabal sources.
  */
class CabalSpellcheckingStrategy extends SpellcheckingStrategy {
  override def isMyContext(element: PsiElement): Boolean = CabalLanguage.Instance.is(element.getLanguage)
}
