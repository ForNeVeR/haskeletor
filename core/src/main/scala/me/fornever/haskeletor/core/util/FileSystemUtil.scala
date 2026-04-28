/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.core.util

import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil

import java.io.File
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.{Files, Paths}

object FileSystemUtil {

  def createDirectoryIfNotExists(directory: File, onlyWriteableByOwner: Boolean): Unit = {
    if (!directory.exists()) {
      val result = FileUtil.createDirectory(directory)
      if (!result && !directory.exists()) {
        throw new RuntimeException(s"Could not create directory `${directory.getAbsolutePath}`")
      }
      if (onlyWriteableByOwner) {
        directory.setWritable(true, true)
        removeGroupWritePermission(directory)
      }
    }
  }

  // On Linux setting `directory.setWritable(true, true)` does not guarantee that group has NO write permissions
  def removeGroupWritePermission(path: File): Unit = {
    if (!SystemInfo.isWindows) {
      val directoryPath = Paths.get(path.getAbsolutePath)
      val permissions = Files.getPosixFilePermissions(directoryPath)
      if (permissions.contains(PosixFilePermission.GROUP_WRITE)) {
        permissions.remove(PosixFilePermission.GROUP_WRITE)
        Files.setPosixFilePermissions(directoryPath, permissions)
      }
    }
  }
}
