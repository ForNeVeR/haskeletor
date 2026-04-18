/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.core.util

import com.intellij.openapi.project.Project
import com.intellij.xml.util.XmlStringUtil
import me.fornever.haskeletor.core.notifications.HaskellNotificationGroup

import scala.collection.mutable.ListBuffer

object StringUtil {

  private final val PackageModuleQualifierPattern = """([a-zA-Z\-]+\-[\.0-9]+\:)?([A-Z][\w\\\-\']*\.)+"""
  private final val PackageQualifierPattern = """^([a-zA-Z\-]+\-[\.0-9]+\:)?"""

  def escapeString(s: String): String = {
    XmlStringUtil.escapeString(s, false, false)
  }

  def removePackageModuleQualifier(s: String): String = {
    s.replaceAll(PackageModuleQualifierPattern, "")
  }

  def removePackageQualifier(s: String): String = {
    s.replaceAll(PackageQualifierPattern, "")
  }

  def sanitizeDeclaration(declaration: String): String = {
    removeCommentsAndWhiteSpaces(declaration.replaceAll(PackageModuleQualifierPattern, ""))
  }

  def removeCommentsAndWhiteSpaces(code: String): String = {
    code.replaceAll("""\{-[^\}]+-\}""", " ").replaceAll("""--.*""", " ").replaceAll("""\s+""", " ")
  }

  def removeOuterParens(name: String): String = {
    if (isWithinParens(name)) {
      name.substring(1, name.length - 1)
    } else {
      name
    }
  }

  def removeOuterQuotes(name: String): String = {
    if (isWithinQuotes(name)) {
      name.substring(1, name.length - 1)
    } else {
      name
    }
  }

  def isWithinQuotes(name: String): Boolean = {
    name.startsWith("'") && name.endsWith("'")
  }

  def isWithinParens(name: String): Boolean = {
    name.startsWith("(") && name.endsWith(")")
  }

  def joinIndentedLines(project: Project, lines: Seq[String]): Seq[String] = {
    if (lines.size == 1) {
      lines
    } else {
      try {
        lines.foldLeft(ListBuffer[StringBuilder]())((lb, s) =>
          if (s.startsWith("  ")) {
            lb.last.append(s)
            lb
          }
          else {
            lb += new StringBuilder(2, s)
          }).map(_.toString).toSeq
      } catch {
        case _: NoSuchElementException =>
          HaskellNotificationGroup.logErrorBalloonEvent(project, s"Could not join indented lines. Probably first line started with spaces. Unexpected input was: ${lines.mkString(", ")}")
          Seq()
      }
    }
  }
}
