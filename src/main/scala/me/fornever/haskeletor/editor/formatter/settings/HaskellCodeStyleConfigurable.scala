/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.editor.formatter.settings

import com.intellij.application.options.{CodeStyleAbstractConfigurable, CodeStyleAbstractPanel, TabbedLanguageCodeStylePanel}
import com.intellij.psi.codeStyle.CodeStyleSettings
import me.fornever.haskeletor.core.HaskellLanguage
import org.jetbrains.annotations.NotNull

class HaskellCodeStyleConfigurable(@NotNull settings: CodeStyleSettings, originalSettings: CodeStyleSettings) extends CodeStyleAbstractConfigurable(settings, originalSettings, "Haskell") {

  protected def createPanel(settings: CodeStyleSettings): CodeStyleAbstractPanel = {
    new HaskellCodeStyleMainPanel(settings, originalSettings)
  }

  override def getHelpTopic: String = {
    null
  }

  class HaskellCodeStyleMainPanel(currentSettings: CodeStyleSettings, settings: CodeStyleSettings) extends TabbedLanguageCodeStylePanel(HaskellLanguage.Instance, currentSettings, settings) {
    protected override def initTabs(settings: CodeStyleSettings): Unit = {
      addIndentOptionsTab(settings)
    }
  }
}
