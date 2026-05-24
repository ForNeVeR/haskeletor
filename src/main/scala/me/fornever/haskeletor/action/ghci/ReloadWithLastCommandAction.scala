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
import me.fornever.haskeletor.runconfig.console.HaskellConsoleViewMap

class ReloadWithLastCommandAction extends AnAction:

  override def update(actionEvent: AnActionEvent): Unit =
    actionEvent.getPresentation.setEnabled(HaskellConsoleViewMap.getConsole(actionEvent.getProject).isDefined)

  def actionPerformed(actionEvent: AnActionEvent): Unit =
    for
      actionContext <- ActionUtil.findActionContext(actionEvent)
      consoleView <- HaskellConsoleViewMap.getConsole(actionContext.project)
    yield
      consoleView.executeCommand(":reload", addToHistory = false)
      consoleView.executeLastCommand()

