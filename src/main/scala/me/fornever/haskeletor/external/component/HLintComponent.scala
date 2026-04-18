/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.external.component

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import me.fornever.haskeletor.HTool
import me.fornever.haskeletor.core.notifications.HaskellNotificationGroup
import me.fornever.haskeletor.external.execution.CommandLine
import me.fornever.haskeletor.settings.HaskellSettingsState
import me.fornever.haskeletor.util.HaskellFileUtil
import spray.json.JsonParser.ParsingException
import spray.json._

object HLintComponent {

  def check(psiFile: PsiFile): Seq[HLintInfo] = {
    StackProjectManager.isHlintAvailable(psiFile.getProject) match {
      case Some(hlintPath) =>
        val project = psiFile.getProject
        val hlintOptions = if (HaskellSettingsState.getHlintOptions.trim.isEmpty) Array[String]() else HaskellSettingsState.getHlintOptions.split("""\s+""")
        HaskellFileUtil.getAbsolutePath(psiFile) match {
          case Some(path) =>
            val output = runHLint(project, hlintPath, hlintOptions.toSeq ++ Seq("--json", path), ignoreExitCode = true)
            if (output.getExitCode > 0 && output.getStderr.nonEmpty) {
              HaskellNotificationGroup.logErrorBalloonEvent(project, s"Error while calling ${HTool.Hlint.name}: ${output.getStderr}")
              Seq()
            } else {
              parseHLintOutput(project, output.getStdout)
            }
          case None => ()
            HaskellNotificationGroup.logWarningBalloonEvent(psiFile.getProject, s"Can not display HLint suggestions because can not determine path for file `${psiFile.getName}`. File exists only in memory")
            Seq()
        }
      case None =>
        HaskellNotificationGroup.logInfoEvent(psiFile.getProject, s"${HTool.Hlint.name} is not (yet) available")
        Seq()
    }
  }

  def versionInfo(project: Project): String = {
    StackProjectManager.isHlintAvailable(project) match {
      case Some(hlintPath) => runHLint(project, hlintPath, Seq("--version"), ignoreExitCode = false).getStdout
      case None => "-"
    }
  }

  private def runHLint(project: Project, hlintPath: String, arguments: Seq[String], ignoreExitCode: Boolean) = {
    CommandLine.run(project, hlintPath, arguments, logOutput = true, ignoreExitCode = ignoreExitCode)
  }

  private object HlintJsonProtocol extends DefaultJsonProtocol {
    implicit val hlintInfoFormat: RootJsonFormat[HLintInfo] = jsonFormat13(HLintInfo)
  }

  import me.fornever.haskeletor.external.component.HLintComponent.HlintJsonProtocol._

  private[external] def parseHLintOutput(project: Project, hlintOutput: String) = {
    if (hlintOutput.trim.isEmpty || hlintOutput == "[]") {
      Seq()
    } else {
      try {
        hlintOutput.parseJson.convertTo[Seq[HLintInfo]]
      } catch {
        case e: ParsingException =>
          HaskellNotificationGroup.logErrorEvent(project, s"Error while parsing HLint output | Message: ${e.getMessage} | HLintOutput: $hlintOutput")
          Seq()
      }
    }
  }
}

case class HLintInfo(module: Seq[String], decl: Seq[String], severity: String, hint: String, file: String, startLine: Int, startColumn: Int, endLine: Int, endColumn: Int, from: String = "", to: Option[String], note: Seq[String], refactorings: String)
