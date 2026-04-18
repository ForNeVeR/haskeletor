/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.external.component

import com.intellij.execution.process.ProcessOutput
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import me.fornever.haskeletor.core.notifications.HaskellNotificationGroup
import me.fornever.haskeletor.external.execution.CommandLine
import me.fornever.haskeletor.psi.{HaskellPsiUtil, HaskellQualifiedNameElement}
import me.fornever.haskeletor.util.{HtmlElement, ScalaFutureUtil}
import me.fornever.haskeletor.{GlobalInfo, HTool}

import java.io.File
import scala.collection.mutable
import scala.concurrent.{Future, blocking}
import scala.jdk.CollectionConverters._

object HoogleComponent {

  private final val HoogleDbName = "hoogle"

  def runHoogle(project: Project, pattern: String, count: Int = 100): Option[Seq[String]] = {
    if (isHoogleFeatureAvailable(project)) {
      ProgressManager.checkCanceled()

      runHoogle(project, Seq(s""""$pattern"""", s"--count=$count")).
        map(o =>
          if (o.getStdoutLines.isEmpty || o.getStdout.contains("No results found"))
            Seq()
          else if (o.getStdoutLines.asScala.last.startsWith("-- ")) {
            o.getStdoutLines.asScala.init.toSeq
          } else {
            o.getStdoutLines.asScala.toSeq
          }
        )
    } else {
      None
    }
  }

  def findDocumentation(project: Project, qualifiedNameElement: HaskellQualifiedNameElement): Option[String] = {
    if (isHoogleFeatureAvailable(project)) {
      ProgressManager.checkCanceled()

      val name = qualifiedNameElement.getIdentifierElement.getName
      val psiFile = qualifiedNameElement.getContainingFile.getOriginalFile
      DefinitionLocationComponent.findDefinitionLocation(psiFile, qualifiedNameElement, None) match {
        case Left(noInfo) =>
          HaskellNotificationGroup.logWarningEvent(project, s"No documentation available as no location info could be found for identifier `$name` due to: ${noInfo.message}")
          None
        case Right(info) =>
          val locationName = info match {
            case PackageModuleLocation(_, _, _, pn) => pn
            case LocalModuleLocation(pf, _, _) => HaskellPsiUtil.findModuleName(pf)
          }
          ProgressManager.checkCanceled()
          HoogleComponent.createDocumentation(project, name, locationName)
      }
    } else {
      Some("No documentation available as Hoogle (database) isn't available")
    }
  }

  private def createDocumentation(project: Project, name: String, locationName: Option[String]): Option[String] = {
    def mkString(lines: mutable.Seq[String]) = {
      lines.mkString("\n").
        replace("<", HtmlElement.Lt).
        replace(">", HtmlElement.Gt).
        replace(" ", HtmlElement.Nbsp).
        replace("\n", HtmlElement.Break)
    }

    ProgressManager.checkCanceled()

    runHoogle(project, Seq("-i", "is:exact", name) ++ locationName.map("+" + _).toSeq).
      flatMap(processOutput =>
        if (processOutput.getStdoutLines.isEmpty || processOutput.getStdout.contains("No results found")) {
          None
        } else {
          val output = processOutput.getStdoutLines(false)
          val (definition, content) = output.asScala.splitAt(2)
          Some(
            DocumentationMarkup.DEFINITION_START +
              mkString(definition) +
              DocumentationMarkup.DEFINITION_END +
              DocumentationMarkup.CONTENT_START +
              HtmlElement.PreStart +
              mkString(content) +
              HtmlElement.PreEnd +
              DocumentationMarkup.CONTENT_END
          )
        }
      )
  }

  private def isHoogleFeatureAvailable(project: Project): Boolean = {
    if (StackProjectManager.isHoogleAvailable(project).isEmpty) {
      HaskellNotificationGroup.logInfoEvent(project, s"${HTool.Hoogle.name} isn't (yet) available")
      false
    } else {
      doesHoogleDatabaseExist(project)
    }
  }

  def doesHoogleDatabaseExist(project: Project): Boolean = {
    hoogleDbPath(project).exists()
  }

  def showHoogleDatabaseDoesNotExistNotification(project: Project): Unit = {
    HaskellNotificationGroup.logInfoBalloonEvent(project, "Hoogle features can be enabled by menu option `Haskell`/`(Re)Build Hoogle database`")
  }

  def versionInfo(project: Project): String = {
    StackProjectManager.isHoogleAvailable(project) match {
      case Some(hooglePath) => CommandLine.run(project, hooglePath, Seq("--version")).getStdout
      case None => "-"
    }
  }

  import scala.concurrent.ExecutionContext.Implicits.global

  private def runHoogle(project: Project, arguments: Seq[String]): Option[ProcessOutput] = {
    ProgressManager.checkCanceled()

    StackProjectManager.isHoogleAvailable(project) match {
      case Some(hooglePath) =>

        ScalaFutureUtil.waitForValue(project,
          Future {
            blocking {
              CommandLine.run(project, hooglePath, Seq(s"--database=${hoogleDbPath(project)}") ++ arguments, logOutput = true)
            }
          }, "runHoogle")
      case None => None
    }
  }

  def hoogleDbPath(project: Project) = {
    new File(GlobalInfo.getIntelliJProjectDirectory(project), HoogleDbName)
  }
}
