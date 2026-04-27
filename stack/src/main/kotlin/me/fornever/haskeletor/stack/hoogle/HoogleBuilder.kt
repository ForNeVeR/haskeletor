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
import com.intellij.openapi.project.guessProjectDir
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.util.containers.orNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.fornever.haskeletor.core.HaskeletorBundle
import me.fornever.haskeletor.core.notifications.HaskellNotificationGroup
import me.fornever.haskeletor.stack.ProjectInfoManager
import me.fornever.haskeletor.stack.StackProcessRunner
import java.nio.file.Path
import kotlin.io.path.pathString

@Service(Service.Level.PROJECT)
class HoogleBuilder(private val project: Project, private val coroutineScope: CoroutineScope) {

    companion object {
        @JvmStatic
        fun getInstance(project: Project): HoogleBuilder = project.service()
    }

    fun launchRebuildHoogle(
        stackExecutable: Path,
        manager: HoogleInstallationManager,
        projectInfoManager: ProjectInfoManager
    ) {
        coroutineScope.launch {
            withBackgroundProgress(project, HaskeletorBundle.message("build.hoogle-database.title")) {
                val haddockBuilt = buildHaddock(stackExecutable, manager)
                if (haddockBuilt) {
                    manager.findHooglePath().orNull()?.let { hoogle ->
                        buildHoogleDatabase(stackExecutable, hoogle, manager, projectInfoManager)
                    }
                }
            }
        }
    }

    private fun getDefaultWorkingDir(): Path =
        project.guessProjectDir()?.toNioPath() ?: error("Cannot determine project directory")

    private suspend fun buildHaddock(stackExecutable: Path, manager: HoogleInstallationManager): Boolean {
        manager.setHaddockBuilding(true)
        try {
            return StackProcessRunner.getInstance(project)
                .executeInBuildView(
                    HaskeletorBundle.message("build.haddock.title"),
                    stackExecutable,
                    getDefaultWorkingDir(),
                    sequenceOf("haddock", "--test", "--no-run-tests", "--no-haddock-hyperlink-source")
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
            StackProcessRunner.getInstance(project)
                .executeInBuildView(
                    HaskeletorBundle.message("build.hoogle-database.title"),
                    stackExecutable,
                    getDefaultWorkingDir(),
                    sequenceOf(
                        hooglePath.pathString,
                        "generate",
                        "--local=${projectInfo.localDocRoot()}",
                        "--local=${projectInfo.snapshotDocRoot()}",
                        "--database=${manager.getHoogleDatabasePath(project)}"
                    )
                )
        } else {
            HaskellNotificationGroup.logErrorBalloonEvent(project, HaskeletorBundle.message("build.hoogle-database.no-project-info"))
        }
    }
}
