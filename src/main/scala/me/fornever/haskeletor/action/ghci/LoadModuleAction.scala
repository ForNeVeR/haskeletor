/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.action.ghci

import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent}
import me.fornever.haskeletor.action.ActionUtil
import me.fornever.haskeletor.core.notifications.HaskellNotificationGroup
import me.fornever.haskeletor.runconfig.console.HaskellConsoleViewMap
import me.fornever.haskeletor.util.HaskellFileUtil

class LoadModuleAction extends AnAction {

  override def update(actionEvent: AnActionEvent): Unit = {
    actionEvent.getPresentation.setEnabled(HaskellConsoleViewMap.getConsole(actionEvent.getProject).isDefined)
  }

  def actionPerformed(actionEvent: AnActionEvent): Unit = {
    for {
      actionContext <- ActionUtil.findActionContext(actionEvent)
      consoleView <- HaskellConsoleViewMap.getConsole(actionContext.project)
    } yield {
      HaskellFileUtil.getAbsolutePath(actionContext.psiFile) match {
        case Some(filePath) => consoleView.executeCommand(s":load $filePath", addToHistory = false)
        case None => HaskellNotificationGroup.logWarningBalloonEvent(actionContext.project, s"Can't load file in REPL because `${actionContext.psiFile.getName}` only exists in memory")
      }
    }
  }

}
