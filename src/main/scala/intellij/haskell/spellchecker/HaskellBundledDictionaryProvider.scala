// SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
// SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
//
// SPDX-License-Identifier: Apache-2.0

package intellij.haskell.spellchecker

import com.intellij.spellchecker.BundledDictionaryProvider

/**
 * Provides a custom dictionary for the Haskell spellchecker.
 */
class HaskellBundledDictionaryProvider extends BundledDictionaryProvider {
  override def getBundledDictionaries: Array[String] = Array("/dictionary/haskell.dic")
}
