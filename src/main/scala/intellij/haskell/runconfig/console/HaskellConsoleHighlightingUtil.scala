// SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
// SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
//
// SPDX-License-Identifier: Apache-2.0

package intellij.haskell.runconfig.console

import scala.util.matching.Regex

object HaskellConsoleHighlightingUtil {
  private val ID = "[A-Z]\\w*"
  private val Module = s"\\*?$ID(\\.$ID)*"
  private val Modules = s"($Module\\s*)*"
  val PromptArrow = ">"
  val LambdaArrow = "λ> "
  val LineWithPrompt = new Regex(s"($Modules$PromptArrow)")
}
