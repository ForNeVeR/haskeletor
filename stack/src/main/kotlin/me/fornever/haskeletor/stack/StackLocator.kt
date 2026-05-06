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
import com.intellij.util.application
import com.intellij.util.text.nullize
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import me.fornever.haskeletor.core.HaskeletorBundle
import me.fornever.haskeletor.settings.HaskellSettingsState
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import kotlin.io.path.Path

@Service(Service.Level.PROJECT)
class StackLocator(private val project: Project, private val environment: EnvironmentAccessor) {

    constructor(project: Project) : this(project, EnvironmentAccessorImpl)

    companion object {
        @JvmStatic
        fun getInstance(project: Project): StackLocator = project.service()
    }

    internal sealed class CacheState {
        object NotCached : CacheState()
        data class Cached(val settingValue: String?, val path: Path?) : CacheState()
    }

    @Volatile
    internal var cacheState: CacheState = CacheState.NotCached

    suspend fun locateStack(): Path? {
        suspend fun loadFromPath(): Path? = withContext(Dispatchers.IO) {
            environment.findExecutableInPath("stack")
        }

        val currentStackPathSetting = HaskellSettingsState.stackPath()
            .map { it.trim() }
            .getOrElse { "" }
            .nullize()

        val cache = cacheState
        if (cache is CacheState.Cached && cache.settingValue == currentStackPathSetting) {
            return cache.path
        }

        // Invalidate the cache:
        val result = currentStackPathSetting?.let(::Path) ?: loadFromPath()
        return result.also {
            cacheState = CacheState.Cached(currentStackPathSetting, result)
        }
    }

    fun locateStackBlocking(): Path? =
        if (application.isDispatchThread) {
            runWithModalProgressBlocking(project, HaskeletorBundle.message("common.progress.locating-stack")) {
                locateStack()
            }
        } else {
            runBlocking {
                locateStack()
            }
        }

    fun locateStackAsFuture(coroutineScope: CoroutineScope): CompletableFuture<Path?> {
        return coroutineScope.async {
            locateStack()
        }.asCompletableFuture()
    }
}

interface EnvironmentAccessor {
    fun findExecutableInPath(baseName: String): Path?
}

object EnvironmentAccessorImpl : EnvironmentAccessor {
    override fun findExecutableInPath(baseName: String): Path? =
        PathEnvironmentVariableUtil.findExecutableInPathOnAnyOS("stack")?.toPath()
}
