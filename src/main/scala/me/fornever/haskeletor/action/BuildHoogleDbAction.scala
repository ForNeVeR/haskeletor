/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.action

import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent}
import com.intellij.openapi.progress.{ProgressIndicator, ProgressManager, Task}
import com.intellij.openapi.project.Project
import me.fornever.haskeletor.external.component.{HoogleComponent, ProjectLibraryBuilder, StackProjectManager}
import me.fornever.haskeletor.util.HaskellEditorUtil

class BuildHoogleDbAction extends AnAction {

  override def update(actionEvent: AnActionEvent): Unit = {
    HaskellEditorUtil.enableExternalAction(actionEvent, (project: Project) =>
      !StackProjectManager.isInitializing(project) &&
        StackProjectManager.isHoogleAvailable(project).isDefined &&
        !ProjectLibraryBuilder.isBuilding(project) &&
        !StackProjectManager.isHaddockBuilding(project))
  }

  def actionPerformed(actionEvent: AnActionEvent): Unit = {
    val message = "Building or rebuilding Hoogle database"
    Option(actionEvent.getProject).foreach(project => {
      ProgressManager.getInstance().run(new Task.Backgroundable(project, message, false) {

        def run(progressIndicator: ProgressIndicator): Unit = {
          HaskellNotificationGroup.logInfoEvent(project, message)
          ProjectLibraryBuilder.resetBuildStatus(project)
          HoogleComponent.rebuildHoogle(project)
        }
      })
    })
  }
}
