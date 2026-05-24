/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.runconfig.console

import com.intellij.execution.console.LanguageConsoleImpl
import com.intellij.execution.process.ColoredProcessHandler

import java.nio.charset.Charset

class HaskellConsoleProcessHandler private[runconfig](val process: Process, val commandLine: String, val console: HaskellConsoleView) extends ColoredProcessHandler(process, commandLine, Charset.forName("UTF-8")):

  def getLanguageConsole: LanguageConsoleImpl = console
