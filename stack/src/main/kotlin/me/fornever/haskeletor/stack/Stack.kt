/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.stack

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import kotlinx.coroutines.*
import java.nio.file.Path
import kotlin.io.path.pathString

object Stack {
    suspend fun executeInBuildView(
        executable: Path,
        workingDirectory: Path,
        arguments: List<String>
    ): Int {
        val buildView = createBuildView()
        currentCoroutineContext().job.invokeOnCompletion {
            cancelBuild(buildView)
        }

        val commandLine = GeneralCommandLine()
            .withExePath(executable.pathString)
            .withWorkDirectory(workingDirectory.pathString)
            .withParameters(arguments)
        val handler = withContext(Dispatchers.IO) {
            OSProcessHandler(commandLine).apply {
                addProcessListener(BuildViewProcessAdapter(buildView))
                coroutineContext.job.invokeOnCompletion {
                    destroyProcess()
                }

                startNotify()
            }
        }

        return handler.waitForTerminationSuspending()
    }
}

private suspend fun ProcessHandler.waitForTerminationSuspending(): Int =
    suspendCancellableCoroutine { continuation ->
        addProcessListener(object : ProcessListener {
            override fun processTerminated(event: ProcessEvent) {
                continuation.resumeWith(Result.success(event.exitCode))
            }
        })
    }
