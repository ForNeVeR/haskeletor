/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.action

import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent}
import com.intellij.openapi.project.Project
import me.fornever.haskeletor.external.component.{ProjectLibraryBuilder, StackProjectManager}
import me.fornever.haskeletor.util.HaskellEditorUtil

class RestartStackReplsAction extends AnAction {

  override def update(actionEvent: AnActionEvent): Unit = {
    HaskellEditorUtil.enableExternalAction(actionEvent, (p: Project) => !StackProjectManager.isInitializing(p) &&
      !ProjectLibraryBuilder.isBuilding(p) &&
      !StackProjectManager.isHaddockBuilding(p) &&
      !StackProjectManager.isPreloadingAllLibraryIdentifiers(p))
  }

  override def actionPerformed(actionEvent: AnActionEvent): Unit = {
    Option(actionEvent.getProject).foreach(project => {
      ProjectLibraryBuilder.resetBuildStatus(project)
      StackProjectManager.getInstance(project).restart()
    })
  }
}
