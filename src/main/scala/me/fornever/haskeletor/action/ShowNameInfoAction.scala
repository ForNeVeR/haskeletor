/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.action

import com.intellij.openapi.actionSystem.{ActionUpdateThread, AnAction, AnActionEvent}
import me.fornever.haskeletor.external.component.NameInfoComponentResult._
import me.fornever.haskeletor.external.component._
import me.fornever.haskeletor.util.HaskellEditorUtil

class ShowNameInfoAction extends AnAction {

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  override def update(actionEvent: AnActionEvent): Unit = {
    HaskellEditorUtil.enableAction(onlyForSourceFile = false, actionEvent)
  }

  def actionPerformed(actionEvent: AnActionEvent): Unit = {
    if (!StackProjectManager.isInitializing(actionEvent.getProject)) {
      ActionUtil.findActionContext(actionEvent).foreach(actionContext => {
        val editor = actionContext.editor
        val psiFile = actionContext.psiFile
        val offset = editor.getCaretModel.getOffset
        Option(psiFile.findElementAt(offset)).foreach(psiElement => {
          val result = HaskellComponentsManager.findNameInfo(psiElement)
          result match {
            case Right(nameInfos) => HaskellEditorUtil.showList(nameInfos.toSeq.map(createInfoText), editor)
            case Left(info) => HaskellEditorUtil.showList(Seq(info.message), editor)
          }
        })
      })
    } else {
      HaskellEditorUtil.showHaskellSupportIsNotAvailableWhileInitializing(actionEvent.getProject)
    }
  }

  private def createInfoText(nameInfo: NameInfo): String = {
    nameInfo match {
      case pi: ProjectNameInfo => s"${pi.declaration}   -- ${pi.filePath}"
      case li: LibraryNameInfo => s"${li.shortenedDeclaration}   -- ${li.moduleName}    ${li.packageName.getOrElse("")}"
      case ii: InfixInfo => ii.declaration
    }
  }

}
