/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor

import com.intellij.openapi.application.PathMacros
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import io.github.soc.directories.ProjectDirectories
import me.fornever.haskeletor.settings.HaskellSettingsState
import me.fornever.haskeletor.util.HaskellFileUtil

import java.io.File

object GlobalInfo {

  final val LibrarySourcedDirName = "lib"
  final val StackWorkDirName = ".stack-work"
  final val StackageLtsVersion = "lts-19"
  private final val ToolsBinDirName = "bin"

  final lazy val DefaultCachePath = {
    // Workaround https://github.com/rikvdkleij/intellij-haskell/issues/503
    if (SystemInfo.isWindows) {
      System.setProperty("jdk.lang.Process.allowAmbiguousCommands", "true")
    }
    ProjectDirectories.from("me.fornever", "", "haskeletor").cacheDir
  }

  lazy val getHaskeletorDirectory: File = {
    val directory = new File(HaskellSettingsState.getCachePath)
    if (directory.exists()) {
      HaskellFileUtil.removeGroupWritePermission(directory)
    } else {
      HaskellFileUtil.createDirectoryIfNotExists(directory, onlyWriteableByOwner = true)
    }
    directory
  }

  lazy val getLibrarySourcesPath: File = {
    new File(getHaskeletorDirectory, GlobalInfo.LibrarySourcedDirName)
  }

  lazy val toolsStackRootPath: File = {
    new File(getHaskeletorDirectory, StackageLtsVersion)
  }

  lazy val toolsBinPath: File = {
    new File(toolsStackRootPath, ToolsBinDirName)
  }

  def toolPath(tool: HTool): File = {
    val name = if (SystemInfo.isWindows) tool.name + ".exe" else tool.name
    new File(toolsBinPath, name)
  }

  def getIntelliJProjectDirectory(project: Project): File = {
    val intelliJProjectDirectory = new File(GlobalInfo.getHaskeletorDirectory, project.getName)
    synchronized {
      if (!intelliJProjectDirectory.exists()) {
        FileUtil.createDirectory(intelliJProjectDirectory)
      }
    }
    intelliJProjectDirectory
  }

  def pathVariables: java.util.Map[String, String] = {
    PathMacros.getInstance.getUserMacros
  }
}
