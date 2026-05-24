/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.action

import com.intellij.openapi.actionSystem.{AnActionEvent, CommonDataKeys}
import com.intellij.openapi.editor.{Editor, SelectionModel}
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilBase

object ActionUtil:

  def findActionContext(actionEvent: AnActionEvent): Option[ActionContext] =
    val context = actionEvent.getDataContext
    for
      editor <- Option(CommonDataKeys.EDITOR.getData(context))
      project <- Option(CommonDataKeys.PROJECT.getData(context))
      psiFile <- Option(PsiUtilBase.getPsiFileInEditor(editor, project))
      selectionModel = Option(editor.getSelectionModel).find(sm => Option(sm.getSelectedText).isDefined)
    yield ActionContext(psiFile, editor, project, selectionModel)

sealed case class ActionContext(psiFile: PsiFile, editor: Editor, project: Project, selectionModel: Option[SelectionModel])
