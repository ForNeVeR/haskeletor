/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.notification

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.{VFileContentChangeEvent, VFileEvent}
import com.intellij.ui.{EditorNotificationPanel, EditorNotifications}
import me.fornever.haskeletor.external.component.StackProjectManager
import me.fornever.haskeletor.util.HaskellProjectUtil

import java.util
import java.util.concurrent.ConcurrentHashMap
import scala.collection.concurrent
import scala.jdk.CollectionConverters._

object ConfigFileWatcherNotificationProvider {
  private val ConfigFileWatcherKey: Key[EditorNotificationPanel] = Key.create("Haskell config file watcher")
  val showNotificationsByProject: concurrent.Map[Project, Boolean] = new ConcurrentHashMap[Project, Boolean]().asScala
}

class ConfigFileWatcherNotificationProvider extends EditorNotifications.Provider[EditorNotificationPanel] {

  override def getKey: Key[EditorNotificationPanel] = ConfigFileWatcherNotificationProvider.ConfigFileWatcherKey

  override def createNotificationPanel(virtualFile: VirtualFile, fileEditor: FileEditor, project: Project): EditorNotificationPanel = {
    if (HaskellProjectUtil.isHaskellProject(project) && ConfigFileWatcherNotificationProvider.showNotificationsByProject.get(project).contains(true)) {
      createPanel(project, virtualFile)
    } else {
      null
    }
  }

  private def createPanel(project: Project, file: VirtualFile): EditorNotificationPanel = {
    val notifications = EditorNotifications.getInstance(project)

    val panel = new EditorNotificationPanel
    panel.setText("Haskell project configuration file is updated")
    panel.createActionLabel("Update Settings and restart REPLs", () => {
      ConfigFileWatcherNotificationProvider.showNotificationsByProject.put(project, false)
      notifications.updateAllNotifications()
      StackProjectManager.restart(project)
    })
    panel.createActionLabel("Ignore", () => {
      ConfigFileWatcherNotificationProvider.showNotificationsByProject.put(project, false)
      notifications.updateAllNotifications()
    })
    panel
  }
}

class ConfigFileWatcher(project: Project, notifications: EditorNotifications) extends BulkFileListener {

  private val watchFileNames = IndexedSeq("stack.yaml", "package.yaml")
  private val watchFileExtensions = IndexedSeq("cabal")

  override def before(events: util.List[_ <: VFileEvent]): Unit = {}

  override def after(events: util.List[_ <: VFileEvent]): Unit = {
    if (!StackProjectManager.isInitializing(project)) {
      if (events.asScala.exists(e =>
        e.isInstanceOf[VFileContentChangeEvent]
        && !e.isFromRefresh
        && (watchFileNames.exists(e.getFile.getName.equalsIgnoreCase) || watchFileExtensions.exists(e.getFile.getExtension.equalsIgnoreCase))
      )) {
        ConfigFileWatcherNotificationProvider.showNotificationsByProject.put(project, true)
        notifications.updateAllNotifications()
      }
    }
  }
}
