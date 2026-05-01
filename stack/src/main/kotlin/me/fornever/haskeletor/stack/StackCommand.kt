/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.stack

import com.intellij.build.BuildProgressListener
import com.intellij.build.BuildViewManager
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.events.FinishBuildEvent
import com.intellij.build.events.StartBuildEvent
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.build.events.impl.SuccessResultImpl
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.*
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.coroutineToIndicator
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import me.fornever.haskeletor.core.HaskeletorBundle
import me.fornever.haskeletor.core.stack.OutputToProgressIndicator
import me.fornever.haskeletor.settings.HaskellSettingsState
import org.jetbrains.annotations.Nls
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.pathString

class StackCommand(
    private val executable: Path,
    val workingDirectory: Path,
    private val arguments: List<String>,
    private val addExtraArguments: Boolean = true
) {

    companion object {
        @JvmStatic
        fun defaultWorkingDir(project: Project): Path =
            project.guessProjectDir()?.toNioPath() ?: error("Cannot determine project directory")
    }

    suspend fun execute(listeners: List<ProcessListener>): Int {
        val fullArguments = if (addExtraArguments) {
            arguments + HaskellSettingsState.getExtraStackArguments()
        } else {
            arguments
        }

        val commandLine = GeneralCommandLine()
            .withExePath(executable.pathString)
            .withWorkDirectory(workingDirectory.pathString)
            .withParameters(fullArguments)
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                OSProcessHandler(commandLine).apply {
                    for (listener in listeners) {
                        addProcessListener(listener)
                    }

                    addProcessListener(object : ProcessListener {
                        override fun processNotStarted() {
                            continuation.resumeWith(Result.failure(RuntimeException("Process not started")))
                        }

                        override fun processTerminated(event: ProcessEvent) {
                            continuation.resumeWith(Result.success(event.exitCode))
                        }
                    })

                    continuation.invokeOnCancellation {
                        destroyProcess()
                    }

                    startNotify()
                }
            }
        }
    }

    suspend fun readOutput(): ProcessOutput {
        val listener = CapturingProcessAdapter()
        execute(listOf(listener))
        return listener.output
    }

    fun readOutputAsFuture(coroutineScope: CoroutineScope): CompletableFuture<ProcessOutput> =
        coroutineScope.async {
            readOutput()
        }.asCompletableFuture()
}

private val globalBuildIdStorage = AtomicInteger()

internal suspend fun StackCommand.executeInBuildView(
    project: Project,
    title: @Nls(capitalization = Nls.Capitalization.Title) String
): Boolean {
    val buildId = globalBuildIdStorage.getAndIncrement()
    val buildDescriptor = DefaultBuildDescriptor(
        buildId,
        title,
        workingDirectory.pathString,
        System.currentTimeMillis()
    )
    val buildViewManager = project.service<BuildViewManager>()

    onBuildStarted(buildViewManager, buildId, buildDescriptor)

    try {
        val exitCode =
            // TODO[#45]: We really want to call reportRawProgress here, but we cannot due to IJPL-243681.
            coroutineToIndicator { indicator ->
                runBlockingCancellable {
                    execute(
                        listOf(
                            BuildViewProcessAdapter(buildViewManager, buildId),
                            OutputToProgressIndicator(title, indicator)
                        )
                    )
                }
            }

        if (exitCode == 0) {
            onBuildSucceeded(buildViewManager, buildId)
            return true
        } else {
            onBuildFailed(buildViewManager, buildId, exitCode)
            return false
        }
    } catch (ex: CancellationException) {
        onBuildCancelled(buildViewManager, buildId)
        throw ex
    }
}

private fun onBuildStarted(listener: BuildProgressListener, buildId: Int, buildDescriptor: DefaultBuildDescriptor) {
    listener.onEvent(
        buildId,
        StartBuildEvent.builder(
            HaskeletorBundle.message("build.messages.build-started"),
            buildDescriptor
        ).build()
    )
}

private fun onBuildCancelled(listener: BuildProgressListener, buildId: Int) {
    listener.onEvent(
        buildId,
        FinishBuildEvent.builder(
            buildId,
            HaskeletorBundle.message("build.messages.build-canceled"),
            FailureResultImpl()
        ).build()
    )
}

private fun onBuildSucceeded(listener: BuildProgressListener, buildId: Int) {
    listener.onEvent(
        buildId,
        FinishBuildEvent.builder(
            buildId,
            HaskeletorBundle.message("build.messages.build-succeeded"),
            SuccessResultImpl()
        ).build()
    )
}

private fun onBuildFailed(listener: BuildProgressListener, buildId: Int, exitCode: Int) {
    listener.onEvent(
        buildId,
        FinishBuildEvent.builder(
            buildId,
            HaskeletorBundle.message("build.messages.build-failed", exitCode),
            FailureResultImpl()
        ).build()
    )
}
