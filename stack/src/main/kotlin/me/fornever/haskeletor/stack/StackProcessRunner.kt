/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.stack

import com.intellij.build.BuildView
import com.intellij.build.BuildViewManager
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.events.FinishBuildEvent
import com.intellij.build.events.StartBuildEvent
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.build.events.impl.SuccessResultImpl
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import org.jetbrains.annotations.Nls
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.pathString

@Service(Service.Level.PROJECT)
class StackProcessRunner(private val project: Project) {

    companion object {
        @JvmStatic
        fun getInstance(project: Project): StackProcessRunner = project.service()
    }

    private val buildIdStorage = AtomicInteger()

    suspend fun executeInBuildView(
        title: @Nls(capitalization = Nls.Capitalization.Title) String,
        stackExecutable: Path,
        workingDirectory: Path,
        arguments: List<String>
    ): Boolean {
        val buildId = buildIdStorage.getAndIncrement()
        val buildDescriptor = DefaultBuildDescriptor(
            buildId,
            title,
            workingDirectory.pathString,
            System.currentTimeMillis()
        )
        val buildView = BuildView(project, buildDescriptor, null, project.service<BuildViewManager>())

        try {
            val commandLine = GeneralCommandLine()
                .withExePath(stackExecutable.pathString)
                .withWorkDirectory(workingDirectory.pathString)
                .withParameters(arguments)
            val handler = withContext(Dispatchers.IO) {
                OSProcessHandler(commandLine).apply {
                    buildView.attachToProcess(this)
                    addProcessListener(BuildViewProcessAdapter(buildView, buildId))
                    coroutineContext.job.invokeOnCompletion {
                        destroyProcess()
                    }

                    startNotify()
                }
            }

            onBuildStarted(buildView, buildId, buildDescriptor)

            val exitCode = handler.waitForTerminationSuspending()
            if (exitCode == 0) {
                onBuildSucceeded(buildView, buildId)
                return true
            } else {
                onBuildFailed(buildView, buildId, exitCode)
                return false
            }
        } catch (ex: CancellationException) {
            onBuildCancelled(buildView, buildId)
            throw ex
        }
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

private fun onBuildStarted(buildView: BuildView, buildId: Int, buildDescriptor: DefaultBuildDescriptor) {
    buildView.onEvent(
        buildId,
        StartBuildEvent.builder(
            "Build started", // TODO: Localize this
            buildDescriptor
        ).build()
    )
}

private fun onBuildCancelled(buildView: BuildView, buildId: Int) {
    buildView.onEvent(
        buildId,
        FinishBuildEvent.builder(
            buildId,
            "Build cancelled", // TODO: Localize this
            FailureResultImpl()
        ).build()
    )
}

private fun onBuildSucceeded(buildView: BuildView, buildId: Int) {
    buildView.onEvent(
        buildId,
        FinishBuildEvent.builder(
            buildId,
            "Build succeeded", // TODO: Localize this
            SuccessResultImpl()
        ).build()
    )
}

private fun onBuildFailed(buildView: BuildView, buildId: Int, exitCode: Int) {
    buildView.onEvent(
        buildId,
        FinishBuildEvent.builder(
            buildId,
            "Build failed with exit code $exitCode.", // TODO: Localize this
            FailureResultImpl()
        ).build()
    )
}
