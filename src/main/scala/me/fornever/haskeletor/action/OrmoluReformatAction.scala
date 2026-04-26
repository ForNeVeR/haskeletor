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
import me.fornever.haskeletor.core.notifications.HaskellNotificationGroup
import me.fornever.haskeletor.external.component.StackProjectManager
import me.fornever.haskeletor.external.execution.CommandLine
import me.fornever.haskeletor.util._

class OrmoluReformatAction extends AnAction {

  override def update(actionEvent: AnActionEvent): Unit = {
    HaskellEditorUtil.enableExternalAction(actionEvent, (project: Project) => StackProjectManager.isOrmoluAvailable(project).isDefined)
  }

  override def actionPerformed(actionEvent: AnActionEvent): Unit = {
    ActionUtil.findActionContext(actionEvent).foreach { actionContext =>
      val psiFile = actionContext.psiFile
      OrmoluReformatAction.reformat(psiFile)
    }
  }
}

object OrmoluReformatAction {

  def reformat(psiFile: PsiFile): Boolean = {
    val project = psiFile.getProject
    StackProjectManager.isOrmoluAvailable(project) match {
      case Some(ormoluPath) =>
        HaskellFileUtil.saveFile(psiFile)

        HaskellFileUtil.getAbsolutePath(psiFile) match {
          case Some(path) =>
            val processOutputFuture = ApplicationManager.getApplication.executeOnPooledThread(ScalaUtil.callable[ProcessOutput] {
              val fileCharset = HaskellFileUtil.getCharset(psiFile)
              CommandLine.run(project, ormoluPath, Seq(path), charset = fileCharset)
            })

            FutureUtil.waitForValue(project, processOutputFuture, s"reformatting by ${HTool.Ormolu.name}") match {
              case None => false
              case Some(processOutput) =>
                if (processOutput.getStderrLines.isEmpty) {
                  HaskellFileUtil.saveFileWithNewContent(psiFile, processOutput.getStdout)
                  true
                } else {
                  HaskellNotificationGroup.logErrorBalloonEvent(project, s"Error while reformatting by `${HTool.Ormolu.name}`. Error: ${processOutput.getStderr}")
                  false
                }
            }
          case None =>
            HaskellNotificationGroup.logWarningBalloonEvent(psiFile.getProject, s"Can not reformat file because could not determine path for file `${psiFile.getName}`. File exists only in memory")
            false
        }
      case None =>
        HaskellNotificationGroup.logWarningBalloonEvent(psiFile.getProject, s"Can not reformat file because `${HTool.Ormolu.name}` is not (yet) available")
        false
    }
  }

  def versionInfo(project: Project): String = {
    StackProjectManager.isOrmoluAvailable(project) match {
      case Some(ormoluPath) => CommandLine.run(project, ormoluPath, Seq("--version")).getStdout
      case None => "-"
    }
  }
}
