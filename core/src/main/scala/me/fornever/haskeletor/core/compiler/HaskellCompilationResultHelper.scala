/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.core.compiler

import com.intellij.openapi.util.NlsSafe
import me.fornever.haskeletor.core.compiler.HaskellCompilationResultHelper.LayoutSpaceChar
import me.fornever.haskeletor.core.util.StringUtil

import java.nio.file.Path

object HaskellCompilationResultHelper {

  private final val ProblemPattern = """((?:[A-Z]:\\)?[^:]+):([\d]+):([\d]+):(.+)""".r
  // Stack status lines like "StateVar > build with ghc-9.0.2" or "Progress 0/67 StateVar > configure"
  private final val StackStatusPattern = """.*\s>\s.*""".r

  final val LayoutSpaceChar = '\u00A0'

  def createCompilationResult(currentFilePath: Path, errorLines: Seq[String], failed: Boolean): CompilationResult = {
    val compilationProblems = errorLines.flatMap(parseErrorLine)

    val (currentFileProblems, otherFileProblems) = compilationProblems.partition(_.filePath == currentFilePath)

    CompilationResult(currentFileProblems, otherFileProblems, failed)
  }

  def parseErrorLine(errorLine: String): Option[CompilationProblem] = {
    // Skip stack status lines (e.g., "StateVar > build with ghc-9.0.2")
    if (StackStatusPattern.matches(errorLine)) {
      return None
    }

    errorLine match {
      case ProblemPattern(filePath, lineNr, columnNr, message) =>
        try {
          val displayMessage = message.trim.replaceAll("""(\s\s\s\s+)""", "\n" + "$1")
          Some(CompilationProblem(Path.of(filePath), lineNr.toInt, columnNr.toInt, displayMessage))
        } catch {
          case _: java.nio.file.InvalidPathException => None
        }
      case _ => None
    }
  }
}

case class CompilationResult(currentFileProblems: Iterable[CompilationProblem], otherFileProblems: Iterable[CompilationProblem], failed: Boolean)

case class CompilationProblem(filePath: Path, lineNr: Int, columnNr: Int, message: String) {

  @NlsSafe
  def plainMessage: String = {
    message.split("\n").mkString.replaceAll("\\s+", " ")
  }

  def htmlMessage: String = {
    StringUtil.escapeString(message.replace(' ', LayoutSpaceChar))
  }

  def isWarning: Boolean = {
    message.startsWith("warning:") || message.startsWith("Warning:")
  }
}


