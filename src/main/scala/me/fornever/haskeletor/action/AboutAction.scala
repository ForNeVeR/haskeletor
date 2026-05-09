/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.action

import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.SystemInfo
import me.fornever.haskeletor.core.intellij.ProjectScope
import me.fornever.haskeletor.external.component.{HLintComponent, HaskellComponentsManager, HoogleComponent, StackProjectManager}
import me.fornever.haskeletor.stack.{StackCommand, StackLocator}
import me.fornever.haskeletor.util.HaskellEditorUtil

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.jdk.FutureConverters.CompletionStageOps

class AboutAction extends AnAction {

  override def update(actionEvent: AnActionEvent): Unit = {
    HaskellEditorUtil.enableExternalAction(actionEvent, !StackProjectManager.isInitializing(_))
  }

  private def boldToolName(name: String): String = {
    if (SystemInfo.isMac) {
      s"<b>$name</b>"
    } else {
      name
    }
  }

  def actionPerformed(actionEvent: AnActionEvent): Unit = {
    Option(actionEvent.getProject).foreach(project => {
      import scala.concurrent.ExecutionContext.Implicits.global

      val futureResult = for {
        stack <- StackLocator.getInstance(project).locateStackAsFuture(ProjectScope.get(project)).asScala.map(Option(_))
        command = stack.map(stack => new StackCommand(
          stack,
          StackCommand.defaultWorkingDir(project),
          IndexedSeq("--numeric-version").asJava,
          false
        ))
        versionOutput <- command.map(_.readOutputAsFuture(ProjectScope.get(project)).asScala.map(Some)).getOrElse(Future.successful(None))
      } yield versionOutput

      futureResult.foreach { versionOutput =>
        ApplicationManager.getApplication.invokeLater(() => {
          val messages = new ArrayBuffer[String]
          val version = versionOutput.map(_.getStdout.trim) match {
            case Some(value) if value.nonEmpty => value
            case None => "-"
          }
          messages.+=(s"${boldToolName("Stack")} version: " + version)
          messages.+=(s"${boldToolName("GHC")}: " + HaskellComponentsManager.getGhcVersion(project).map(_.prettyString).getOrElse("-") + "\n")
          messages.+=(s"${boldToolName("HLint")}: " + HLintComponent.versionInfo(project))
          messages.+=(s"${boldToolName("Hoogle")}: " + HoogleComponent.versionInfo(project))
          messages.+=(s"${boldToolName("Ormolu")}: " + OrmoluReformatAction.versionInfo(project))
          messages.+=(s"${boldToolName("Stylish-haskell")}: " + StylishHaskellReformatAction.versionInfo(project))
          Messages.showInfoMessage(project, messages.mkString("\n"), "About Haskell Project")
        })
      }
    })
  }
}
