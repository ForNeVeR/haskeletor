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
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.util.AbstractProgressIndicatorBase
import com.intellij.openapi.project.Project
import com.intellij.platform.util.progress.RawProgressReporter
import com.intellij.platform.util.progress.reportRawProgress
import kotlinx.coroutines.*
import me.fornever.haskeletor.core.stack.OutputToProgressIndicator
import org.jetbrains.annotations.Nls
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.pathString

@Service(Service.Level.PROJECT)
internal class StackProcessRunner(private val project: Project) {

    companion object {
        @JvmStatic
        fun getInstance(project: Project): StackProcessRunner = project.service()
    }

    private val buildIdStorage = AtomicInteger()

    suspend fun executeInBuildView(
        title: @Nls(capitalization = Nls.Capitalization.Title) String,
        stackExecutable: Path,
        workingDirectory: Path,
        arguments: Sequence<String>
    ): Boolean {
        val buildId = buildIdStorage.getAndIncrement()
        val buildDescriptor = DefaultBuildDescriptor(
            buildId,
            title,
            workingDirectory.pathString,
            System.currentTimeMillis()
        )
        val buildViewManager = project.service<BuildViewManager>()

        onBuildStarted(buildViewManager, buildId, buildDescriptor)

        try {
            val commandLine = GeneralCommandLine()
                .withExePath(stackExecutable.pathString)
                .withWorkDirectory(workingDirectory.pathString)
                .withParameters(arguments.toList())
            val exitCode = withContext(Dispatchers.IO) {
                @Suppress("UnstableApiUsage")
                reportRawProgress { reporter ->
                    suspendCancellableCoroutine { continuation ->
                        OSProcessHandler(commandLine).apply {
                            addProcessListener(BuildViewProcessAdapter(buildViewManager, buildId))
                            addProcessListener(OutputToProgressIndicator(title, getProgressIndicator(reporter)))
                            addProcessListener(object : ProcessListener {
                                override fun processNotStarted() {
                                    continuation.resumeWith(Result.failure(RuntimeException("Process not started")))
                                }

                                override fun processTerminated(event: ProcessEvent) {
                                    continuation.resumeWith(Result.success(event.exitCode))
                                }
                            })
                            coroutineContext.job.invokeOnCompletion {
                                destroyProcess()
                            }

                            startNotify()
                        }
                    }
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
}

private fun onBuildStarted(listener: BuildProgressListener, buildId: Int, buildDescriptor: DefaultBuildDescriptor) {
    listener.onEvent(
        buildId,
        StartBuildEvent.builder(
            "Build started", // TODO: Localize this
            buildDescriptor
        ).build()
    )
}

private fun onBuildCancelled(listener: BuildProgressListener, buildId: Int) {
    listener.onEvent(
        buildId,
        FinishBuildEvent.builder(
            buildId,
            "Build cancelled", // TODO: Localize this
            FailureResultImpl()
        ).build()
    )
}

private fun onBuildSucceeded(listener: BuildProgressListener, buildId: Int) {
    listener.onEvent(
        buildId,
        FinishBuildEvent.builder(
            buildId,
            "Build succeeded", // TODO: Localize this
            SuccessResultImpl()
        ).build()
    )
}

private fun onBuildFailed(listener: BuildProgressListener, buildId: Int, exitCode: Int) {
    listener.onEvent(
        buildId,
        FinishBuildEvent.builder(
            buildId,
            "Build failed with exit code $exitCode.", // TODO: Localize this
            FailureResultImpl()
        ).build()
    )
}

@Suppress("UnstableApiUsage")
private fun getProgressIndicator(reporter: RawProgressReporter): ProgressIndicator {
    return object : AbstractProgressIndicatorBase() {
        override fun setText(text: String?) {
            reporter.text(text)
        }

        override fun setText2(text: String?) {
            reporter.details(text)
        }

        override fun setFraction(fraction: Double) {
            reporter.fraction(fraction)
        }
    }
}
