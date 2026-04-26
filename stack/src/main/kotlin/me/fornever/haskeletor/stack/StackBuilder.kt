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
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.coroutineToIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportRawProgress
import com.intellij.platform.util.progress.withProgressText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.fornever.haskeletor.core.HaskeletorBundle
import me.fornever.haskeletor.core.notifications.HaskellNotificationGroup
import org.jetbrains.annotations.Nls
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class StackBuilder(private val project: Project, private val coroutineScope: CoroutineScope) {

    companion object {
        @JvmStatic
        fun getInstance(project: Project): StackBuilder = project.service()
    }

    fun launchBuildWorkflow(
        stackExecutable: Path,
        libraryTargets: () -> List<String>,
        ghcOptions: () -> List<String>,
        finishingAction: (ProgressIndicator) -> Unit
    ) {
        coroutineScope.launch {
            withBackgroundProgress(project, HaskeletorBundle.message("workflow.build-project-with-dependencies.title")) {
                val stack = StackProcessRunner.getInstance(project)
                val ghcOptions = ghcOptions().asSequence()

                val dependencyBuildStatus = stack.buildDependenciesInBuildView(stackExecutable, ghcOptions)

                if (dependencyBuildStatus) {
                    val projectLibTargets = libraryTargets()
                    stack.build(
                        HaskeletorBundle.message("build.project-libraries.title"),
                        stackExecutable,
                        projectLibTargets.asSequence(),
                        ghcOptions
                    )
                } else {
                    HaskellNotificationGroup.logErrorBalloonEvent(project, HaskeletorBundle.message("notification.dependencies-failed.text"))
                }

                coroutineToIndicator(finishingAction)
            }
        }
    }

    private suspend fun StackProcessRunner.build(
        title: @Nls(capitalization = Nls.Capitalization.Title) String,
        stackExecutable: Path,
        buildArguments: Sequence<String>,
        ghcOptions: Sequence<String>
    ) =
        executeInBuildView(
            title,
            stackExecutable,
            project.guessProjectDir()?.toNioPath() ?: error("Cannot determine the project directory."),
            sequenceOf("build", "--fast", "--progress-bar", "full", "--no-interleaved-output")
                + buildArguments
                + ghcOptions
        )

    private suspend fun StackProcessRunner.buildDependenciesInBuildView(
        stackExecutable: Path,
        ghcOptions: Sequence<String>
    ) =
        build(
            HaskeletorBundle.message("build.project-dependencies.title"),
            stackExecutable,
            sequenceOf("--test", "--bench", "--no-run-tests", "--no-run-benchmarks", "--only-dependencies"),
            ghcOptions
        )

}
