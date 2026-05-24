/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.editor.formatter.settings

import com.intellij.psi.codeStyle.{CodeStyleSettings, CustomCodeStyleSettings}

class HaskellCodeStyleSettings(settings: CodeStyleSettings) extends CustomCodeStyleSettings("HaskellCodeStyleSettings", settings):
  settings.AUTODETECT_INDENTS = false
