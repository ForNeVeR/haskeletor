// SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
// SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
//
// SPDX-License-Identifier: Apache-2.0

package intellij.haskell.runconfig.console

import java.nio.charset.Charset

import com.intellij.execution.console.LanguageConsoleImpl
import com.intellij.execution.process.ColoredProcessHandler

class HaskellConsoleProcessHandler private[runconfig](val process: Process, val commandLine: String, val console: HaskellConsoleView) extends ColoredProcessHandler(process, commandLine, Charset.forName("UTF-8")) {

  def getLanguageConsole: LanguageConsoleImpl = console
}
