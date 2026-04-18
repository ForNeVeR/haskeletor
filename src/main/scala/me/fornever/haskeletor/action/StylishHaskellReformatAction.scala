/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.action

import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import me.fornever.haskeletor.HTool
import me.fornever.haskeletor.external.component.StackProjectManager
import me.fornever.haskeletor.external.execution.CommandLine
import me.fornever.haskeletor.util.{FutureUtil, HaskellEditorUtil, HaskellFileUtil, ScalaUtil}

class StylishHaskellReformatAction extends AnAction {

  override def update(actionEvent: AnActionEvent): Unit = {
    HaskellEditorUtil.enableExternalAction(actionEvent, (project: Project) => StackProjectManager.isStylishHaskellAvailable(project).isDefined)
  }

  override def actionPerformed(actionEvent: AnActionEvent): Unit = {
    ActionUtil.findActionContext(actionEvent).foreach(actionContext => {
      StylishHaskellReformatAction.reformat(actionContext.psiFile)
    })
  }
}

object StylishHaskellReformatAction {

  def versionInfo(project: Project): String = {
    StackProjectManager.isStylishHaskellAvailable(project) match {
      case Some(stylishHaskellPath) => CommandLine.run(project, stylishHaskellPath, Seq("--version")).getStdout
      case None => "-"
    }
  }

  private[action] def reformat(psiFile: PsiFile): Unit = {
    val project = psiFile.getProject
    StackProjectManager.isStylishHaskellAvailable(project) match {
      case Some(stylishHaskellPath) =>

        HaskellFileUtil.saveFile(psiFile)

        HaskellFileUtil.getAbsolutePath(psiFile) match {
          case Some(path) =>
            val processOutputFuture = ApplicationManager.getApplication.executeOnPooledThread(ScalaUtil.callable[ProcessOutput] {
              val fileCharset = HaskellFileUtil.getCharset(psiFile)
              CommandLine.run(project, stylishHaskellPath, Seq(path), charset = fileCharset)
            })

            FutureUtil.waitForValue(project, processOutputFuture, s"reformatting by ${HTool.StylishHaskell.name}") match {
              case None => ()
              case Some(processOutput) =>
                if (processOutput.getStderrLines.isEmpty) {
                  HaskellFileUtil.saveFileWithNewContent(psiFile, processOutput.getStdout)
                } else {
                  HaskellNotificationGroup.logInfoEvent(project, s"Error while reformatting by `${HTool.StylishHaskell.name}`. Error: ${processOutput.getStderr}")
                }
            }
          case None => HaskellNotificationGroup.logWarningBalloonEvent(psiFile.getProject, s"Can not reformat file because could not determine path for file `${psiFile.getName}`. File exists only in memory")
        }
      case None => ()
    }
  }
}
