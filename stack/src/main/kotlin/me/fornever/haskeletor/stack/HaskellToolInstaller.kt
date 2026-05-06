/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.stack

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.platform.ide.progress.withBackgroundProgress
import kotlinx.coroutines.*
import me.fornever.haskeletor.core.HaskeletorBundle
import me.fornever.haskeletor.core.notifications.HaskellNotificationGroup
import me.fornever.haskeletor.settings.GlobalInfo
import me.fornever.haskeletor.settings.HTool
import me.fornever.haskeletor.settings.HaskellSettingsState
import kotlin.io.path.exists
import kotlin.io.path.pathString

@Service(Service.Level.PROJECT)
class HaskellToolInstaller(private val project: Project, private val coroutineScope: CoroutineScope) {

    companion object {
        @JvmStatic
        fun getInstance(project: Project): HaskellToolInstaller = project.service()
    }

    private suspend fun updateStackIndex() {
        val result = StackCommand(
            StackLocator.getInstance(project).locateStack() ?: return,
            StackCommand.defaultWorkingDir(project),
            listOf("update"),
            addExtraArguments = false
        ).execute(emptyList())
        if (result != 0) {
            HaskellNotificationGroup.logErrorBalloonEvent(project, HaskeletorBundle.message("notification.update-stack-index-failed.text"))
        }
    }

    private suspend fun installTools(
        useSystemGhc: Boolean,
        forceMakeChanges: Boolean
    ) {
        if (HaskellSettingsState.useCustomTools()) return

        val tools = listOf(
            HTool.`Hlint$`.`MODULE$`,
            HTool.`Hoogle$`.`MODULE$`,
            HTool.`Ormolu$`.`MODULE$`,
            HTool.`StylishHaskell$`.`MODULE$`,
        )

        suspend fun toolInstalled(tool: HTool): Boolean =
            withContext(Dispatchers.IO) {
                val path = GlobalInfo.toolPath(tool).toPath()
                path.exists()
            }

        suspend fun installTool(tool: HTool) {
            val systemGhcOption = if (useSystemGhc) listOf("--system-ghc") else emptyList()
            val cpuCoresCount = Runtime.getRuntime().availableProcessors()
            val jobsCount = if (cpuCoresCount > 2) (cpuCoresCount / 2) + 1 else 1
            val arguments = systemGhcOption + listOf(
                "--terminal",
                "--color", "never",
                "-j$jobsCount",
                "--stack-root", GlobalInfo.toolsStackRootPath().toPath().pathString,
                "--resolver", GlobalInfo.StackageLtsVersion(),
                "--local-bin-path", GlobalInfo.toolsBinPath().toPath().pathString,
                "install", tool.name(),
                "--progress-bar", "full",
                "--no-interleaved-output"
            )

            val processOutput = StackCommand(
                StackLocator.getInstance(project).locateStack() ?: return,
                StackCommand.defaultWorkingDir(project),
                arguments,
                addExtraArguments = false
            ).executeWithProgress(HaskeletorBundle.message("progress.installing-tool.title", tool.name()))
            if (processOutput.exitCode != 0) {
                ToolInstallFailureBuildView.getInstance(project)
                    .showFailure(tool.name(), processOutput.stderr)
            }
        }

        for (name in tools) {
            if (forceMakeChanges || !toolInstalled(name))
                installTool(name)
        }
    }

    fun launchInstallHaskellTools(
        forceMakeChanges: Boolean,
        useSystemGhc: Boolean,
        title: @NlsContexts.ProgressTitle String,
        afterUpdate: Runnable,
        finallyAction: Runnable
    ) {
        coroutineScope.launch {
            try {
                withBackgroundProgress(
                    project,
                    title
                ) {
                    if (forceMakeChanges) {
                        updateStackIndex()
                    }

                    installTools(useSystemGhc, forceMakeChanges)

                    afterUpdate.run()
                }
            } finally {
                finallyAction.run()
            }
        }
    }
}

private val logger = logger<HaskellToolInstaller>()
