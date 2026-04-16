/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.action

import com.intellij.openapi.actionSystem.{ActionUpdateThread, AnAction, AnActionEvent}
import me.fornever.haskeletor.annotator.HaskellAnnotator
import me.fornever.haskeletor.util.HaskellEditorUtil

class ShowProblemMessageAction extends AnAction {

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  override def update(actionEvent: AnActionEvent): Unit = {
    HaskellEditorUtil.enableAction(onlyForSourceFile = true, actionEvent)
  }

  override def actionPerformed(actionEvent: AnActionEvent): Unit = {
    ActionUtil.findActionContext(actionEvent).foreach(actionContext => {
      val project = actionContext.project
      val editor = actionContext.editor
      val offset = editor.getCaretModel.getOffset
      HaskellAnnotator.getHighlightingTooltipHtml(project, offset, editor).foreach(info => {
        HaskellEditorUtil.showHint(editor, info)
      })
    })
  }
}
