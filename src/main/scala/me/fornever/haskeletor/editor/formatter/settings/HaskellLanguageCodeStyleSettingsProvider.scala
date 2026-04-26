/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.editor.formatter.settings

import com.intellij.application.options.SmartIndentOptionsEditor
import com.intellij.lang.Language
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider.SettingsType
import com.intellij.psi.codeStyle.{CommonCodeStyleSettings, LanguageCodeStyleSettingsProvider}
import me.fornever.haskeletor.core.HaskellLanguage
import org.jetbrains.annotations.NotNull

class HaskellLanguageCodeStyleSettingsProvider extends LanguageCodeStyleSettingsProvider {

  @NotNull
  override def getLanguage: Language = {
    HaskellLanguage.Instance
  }

  override def customizeDefaults(commonSettings: CommonCodeStyleSettings, indentOptions: CommonCodeStyleSettings.IndentOptions): Unit = {
    indentOptions.INDENT_SIZE = 2
    indentOptions.CONTINUATION_INDENT_SIZE = 4
    indentOptions.TAB_SIZE = 2
    indentOptions.USE_TAB_CHARACTER = false
  }

  override def getIndentOptionsEditor: SmartIndentOptionsEditor = {
    new SmartIndentOptionsEditor(this)
  }

  override def getCodeSample(settingsType: SettingsType): String =
    """-- Reformatting is done externally by Ormolu.
      |-- Setting code style options here has no effect.
    """.stripMargin
}
