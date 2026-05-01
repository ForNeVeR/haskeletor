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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.withContext
import me.fornever.haskeletor.core.HaskeletorBundle
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

@Service(Service.Level.PROJECT)
class StackLocator(private val project: Project) {

    companion object {
        @JvmStatic
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

    fun locateStackAsFuture(coroutineScope: CoroutineScope): CompletableFuture<Path?> {
        return coroutineScope.async {
            locateStack()
        }.asCompletableFuture()
    }
}
