// SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
// SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
//
// SPDX-License-Identifier: Apache-2.0

package intellij.haskell.action.ghci

import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent}
import intellij.haskell.action.ActionUtil
import intellij.haskell.runconfig.console.HaskellConsoleViewMap

class ReloadWithLastCommandAction extends AnAction {

  override def update(actionEvent: AnActionEvent): Unit = {
    actionEvent.getPresentation.setEnabled(HaskellConsoleViewMap.getConsole(actionEvent.getProject).isDefined)
  }

  def actionPerformed(actionEvent: AnActionEvent): Unit = {
    for {
      actionContext <- ActionUtil.findActionContext(actionEvent)
      consoleView <- HaskellConsoleViewMap.getConsole(actionContext.project)
    } yield {
      consoleView.executeCommand(":reload", addToHistory = false)
      consoleView.executeLastCommand()
    }
  }

}
