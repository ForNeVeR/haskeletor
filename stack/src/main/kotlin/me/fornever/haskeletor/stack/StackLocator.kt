/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.stack

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import me.fornever.haskeletor.core.HaskeletorBundle
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class StackLocator(private val project: Project) {

    companion object {
        fun getInstance(project: Project): StackLocator = project.service()
    }

    suspend fun locateStack(): Path? {
        TODO("Load from settings if overridden")
        TODO("Seek in PATH")
        TODO("Cache")
    }

    fun locateStackBlocking(): Path? =
        runWithModalProgressBlocking(project, HaskeletorBundle.message("common.progress.locating-stack")) {
            locateStack()
        }
}
