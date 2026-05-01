/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.stack.hoogle

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.util.containers.orNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.fornever.haskeletor.core.HaskeletorBundle
import me.fornever.haskeletor.core.notifications.HaskellNotificationGroup
import me.fornever.haskeletor.stack.ProjectInfoManager
import me.fornever.haskeletor.stack.StackCommand
import me.fornever.haskeletor.stack.StackLocator
import me.fornever.haskeletor.stack.executeInBuildView
import java.nio.file.Path
import kotlin.io.path.pathString

@Service(Service.Level.PROJECT)
class HoogleBuilder(private val project: Project, private val coroutineScope: CoroutineScope) {

    companion object {
        @JvmStatic
        fun getInstance(project: Project): HoogleBuilder = project.service()
    }

    fun launchRebuildHoogle(
        manager: HoogleInstallationManager,
        projectInfoManager: ProjectInfoManager
    ) {
        coroutineScope.launch {
            val stack = StackLocator.getInstance(project).locateStack() ?: return@launch
            withBackgroundProgress(project, HaskeletorBundle.message("build.hoogle-database.title")) {
                val haddockBuilt = buildHaddock(stack, manager)
                if (haddockBuilt) {
                    manager.findHooglePath().orNull()?.let { hoogle ->
                        buildHoogleDatabase(stack, hoogle, manager, projectInfoManager)
                    }
                }
            }
        }
    }

    private suspend fun buildHaddock(stackExecutable: Path, manager: HoogleInstallationManager): Boolean {
        manager.setHaddockBuilding(true)
        try {
            return StackCommand(
                stackExecutable,
                StackCommand.defaultWorkingDir(project),
                listOf("haddock", "--test", "--no-run-tests", "--no-haddock-hyperlink-source")
            ).executeInBuildView(
                project,
                HaskeletorBundle.message("build.haddock.title"),
            )
        } finally {
            manager.setHaddockBuilding(false) // TODO[#44]: Technically a race condition: concurrent Haddock builds will wreak havoc.
        }
    }

    private suspend fun buildHoogleDatabase(
        stackExecutable: Path,
        hooglePath: Path,
        manager: HoogleInstallationManager,
        projectInfoManager: ProjectInfoManager
    ) {
        val projectInfo = projectInfoManager.findGlobalProjectInfo().orNull()
        if (projectInfo != null) {
            StackCommand(
                stackExecutable,
                StackCommand.defaultWorkingDir(project),
                listOf(
                    hooglePath.pathString,
                    "generate",
                    "--local=${projectInfo.localDocRoot()}",
                    "--local=${projectInfo.snapshotDocRoot()}",
                    "--database=${manager.getHoogleDatabasePath(project)}"
                )
            ).executeInBuildView(
                project,
                HaskeletorBundle.message("build.hoogle-database.title")
            )
        } else {
            HaskellNotificationGroup.logErrorBalloonEvent(project, HaskeletorBundle.message("build.hoogle-database.no-project-info"))
        }
    }
}
