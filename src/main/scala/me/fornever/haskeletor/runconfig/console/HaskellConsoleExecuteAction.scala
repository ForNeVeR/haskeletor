/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.runconfig.console

import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent, CommonDataKeys}
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx

class HaskellConsoleExecuteAction extends AnAction:

  override def update(actionEvent: AnActionEvent): Unit =
    val presentation = actionEvent.getPresentation
    val editor: Editor = actionEvent.getData(CommonDataKeys.EDITOR)
    if !editor.isInstanceOf[EditorEx] || editor.asInstanceOf[EditorEx].isRendererMode then
      presentation.setEnabled(false)
    else
      HaskellConsoleViewMap.getConsole(editor) match
        case Some(consoleView) => presentation.setEnabledAndVisible(consoleView.isRunning)
        case None => presentation.setEnabled(false)

  override def actionPerformed(actionEvent: AnActionEvent): Unit =
    for
      editor <- Option(actionEvent.getData(CommonDataKeys.EDITOR))
      consoleView <- HaskellConsoleViewMap.getConsole(editor)
    yield consoleView.execute()
