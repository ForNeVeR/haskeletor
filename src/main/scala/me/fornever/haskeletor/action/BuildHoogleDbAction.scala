/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.action

import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent}
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import me.fornever.haskeletor.core.notifications.HaskellNotificationGroup
import me.fornever.haskeletor.core.project.GlobalProjectInfo
import me.fornever.haskeletor.external.component.{GlobalProjectInfoComponent, HoogleComponent, ProjectLibraryBuilder, StackProjectManager}
import me.fornever.haskeletor.sdk.HaskellSdkType
import me.fornever.haskeletor.stack.ProjectInfoManager
import me.fornever.haskeletor.stack.hoogle.{HoogleBuilder, HoogleInstallationManager}
import me.fornever.haskeletor.util.HaskellEditorUtil

import java.nio.file.Path
import java.util.Optional

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
      ProjectLibraryBuilder.resetBuildStatus(project)
      HaskellNotificationGroup.logInfoEvent(project, message)

      HaskellSdkType.getStackPath(project).foreach(stack => {
        HoogleBuilder.getInstance(project).launchRebuildHoogle(
          Path.of(stack),
          project.getService(classOf[HoogleInstallationManagerImpl]),
          project.getService(classOf[ProjectInfoManagerImpl])
        )
      })
    })
  }
}

@Service(Array(Service.Level.PROJECT))
private class HoogleInstallationManagerImpl(project: Project) extends HoogleInstallationManager {

  override def findHooglePath(): Optional[Path] =
    Optional.ofNullable(StackProjectManager.isHoogleAvailable(project).orNull)
  override def getHoogleDatabasePath(project: Project): Path =
    HoogleComponent.hoogleDbPath(project).toPath
  override def setHaddockBuilding(building: Boolean): Unit =
    StackProjectManager.setHaddockBuilding(project, building)
}

@Service(Array(Service.Level.PROJECT))
private class ProjectInfoManagerImpl(project: Project) extends ProjectInfoManager {

  override def findGlobalProjectInfo(): Optional[GlobalProjectInfo] =
    Optional.ofNullable(GlobalProjectInfoComponent.findGlobalProjectInfo(project).orNull)
}
