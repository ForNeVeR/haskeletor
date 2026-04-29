/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.stack

import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.fornever.haskeletor.core.HaskeletorBundle
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class StackLocator(private val project: Project) {

    companion object {
        fun getInstance(project: Project): StackLocator = project.service()
    }

    suspend fun locateStack(): Path? {
        fun loadFromCache(): Path? = TODO("Cache")
        fun loadFromSettings(): Path? = TODO("Load from settings if overridden")
        suspend fun loadFromPath(): Path? = withContext(Dispatchers.IO) {
            PathEnvironmentVariableUtil.findExecutableInPathOnAnyOS("stack")?.toPath()
        }

        return loadFromCache()
            ?: loadFromSettings()
            ?: loadFromPath()
    }

    fun locateStackBlocking(): Path? =
        runWithModalProgressBlocking(project, HaskeletorBundle.message("common.progress.locating-stack")) {
            locateStack()
        }
}
