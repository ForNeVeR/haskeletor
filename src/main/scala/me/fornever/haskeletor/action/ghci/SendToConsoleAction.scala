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
import me.fornever.haskeletor.psi.{HaskellPsiUtil, HaskellTypes}
import me.fornever.haskeletor.runconfig.console.HaskellConsoleViewMap

class SendToConsoleAction extends AnAction:

  override def update(actionEvent: AnActionEvent): Unit =
    actionEvent.getPresentation.setEnabled(HaskellConsoleViewMap.getConsole(actionEvent.getProject).isDefined)

  def actionPerformed(actionEvent: AnActionEvent): Unit =
    ActionUtil.findActionContext(actionEvent).foreach(actionContext => {
      val editor = actionContext.editor
      val psiFile = actionContext.psiFile

      val lookupText = Option(editor.getSelectionModel.getSelectedText).orElse {
        HaskellPsiUtil.untilNonWhitespaceBackwards(Option(psiFile.findElementAt(editor.getCaretModel.getOffset))).map {
          case e if e.getNode.getElementType == HaskellTypes.HS_COMMENT =>
            e.getText.stripPrefix("--").trim()

          case e if e.getNode.getElementType == HaskellTypes.HS_NCOMMENT =>
            e.getText.stripPrefix("{-").stripSuffix("-}").trim()

          case e =>
            e.getText
        }
      }

      for
        text <- lookupText
        console <- HaskellConsoleViewMap.getConsole(actionContext.project)
      yield {
        console.executeCommand(text)
      }
    })

