/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.external.component

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.{VFileContentChangeEvent, VFileEvent}
import me.fornever.haskeletor.external.repl.StackRepl.LibType
import me.fornever.haskeletor.util.{HaskellFileUtil, HaskellProjectUtil}

import java.util
import scala.jdk.CollectionConverters.*

class ProjectLibraryFileWatcher(project: Project) extends BulkFileListener:

  override def before(events: util.List[? <: VFileEvent]): Unit = {}

  override def after(events: util.List[? <: VFileEvent]): Unit =
    if !project.isDisposed then
      val componentTargets = (for
        virtualFile <- events.asScala.filter(e => e.isInstanceOf[VFileContentChangeEvent] && HaskellFileUtil.isHaskellFile(e.getFile) && HaskellProjectUtil.isSourceFile(project, e.getFile)).map(_.getFile)
        componentTarget <- HaskellComponentsManager.findComponentTarget(project, HaskellFileUtil.getAbsolutePath(virtualFile))
        if componentTarget.stanzaType == LibType
      yield componentTarget).toSet

      if componentTargets.nonEmpty then
        ProjectLibraryBuilder.addBuild(project, componentTargets)
