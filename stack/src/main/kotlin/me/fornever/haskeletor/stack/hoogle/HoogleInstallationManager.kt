/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.stack.hoogle

import com.intellij.openapi.project.Project
import java.nio.file.Path
import java.util.*

interface HoogleInstallationManager {
    fun findHooglePath(): Optional<Path>
    fun getHoogleDatabasePath(project: Project): Path
    fun setHaddockBuilding(building: Boolean)
}
