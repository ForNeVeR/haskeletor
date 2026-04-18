/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.external.execution

import com.intellij.concurrency.JobSchedulerImpl.getCPUCoresCount
import com.intellij.execution.process._
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.WaitFor
import me.fornever.haskeletor.core.HaskeletorBundle
import me.fornever.haskeletor.core.notifications.HaskellNotificationGroup
import me.fornever.haskeletor.sdk.HaskellSdkType
import me.fornever.haskeletor.settings.HaskellSettingsState
import me.fornever.haskeletor.stackyaml.StackYamlComponent
import me.fornever.haskeletor.util.HaskellProjectUtil
import org.jetbrains.annotations.Nls

import scala.jdk.CollectionConverters._

object StackCommandLine {

  final val NoDiagnosticsShowCaretFlag = "-fno-diagnostics-show-caret"

  def stackVersion(project: Project): Option[String] = {
    StackCommandLine.run(project, Seq("--numeric-version"), enableExtraArguments = false).flatMap(_.getStdoutLines.asScala.headOption)
  }

  def run(project: Project, arguments: Seq[String], timeoutInMillis: Long = CommandLine.DefaultTimeout.toMillis,
          ignoreExitCode: Boolean = false, logOutput: Boolean = false, workDir: Option[String] = None, notifyBalloonError: Boolean = false, enableExtraArguments: Boolean = true): Option[ProcessOutput] = {
    HaskellSdkType.getStackPath(project).map(stackPath => {
      CommandLine.runInWorkDir(
        project,
        workDir.getOrElse(project.getBasePath),
        stackPath,
        arguments ++ (if (enableExtraArguments) HaskellSettingsState.getExtraStackArguments else Seq()),
        timeoutInMillis.toInt,
        ignoreExitCode = ignoreExitCode,
        logOutput = logOutput,
        notifyBalloonError = notifyBalloonError
      )
    })
  }

  def runWithProgressIndicator(project: Project,
                               workDir: Option[String],
                               arguments: Seq[String],
                               @Nls title: String,
                               progressIndicator: Option[ProgressIndicator]): Option[CapturingProcessHandler] = {
    HaskellSdkType.getStackPath(project).map(stackPath => {
      CommandLine.runWithProgressIndicator(
        project,
        workDir,
        stackPath,
        arguments,
        title,
        progressIndicator
      )
    })
  }

  def installTool(project: Project, progressIndicator: ProgressIndicator, toolName: String): Boolean = {
    import me.fornever.haskeletor.GlobalInfo._
    val systemGhcOption = if (StackYamlComponent.isNixEnabled(project) || !HaskellSettingsState.useSystemGhc) {
      Seq()
    } else {
      Seq("--system-ghc")
    }

    val cpuCoresCount = getCPUCoresCount
    val jobsCount = if (cpuCoresCount > 2) (cpuCoresCount / 2) + 1 else 1
    val arguments = systemGhcOption ++ Seq(
      "--terminal",
      "--color", "never",
      s"-j$jobsCount",
      "--stack-root", toolsStackRootPath.getPath,
      "--resolver", StackageLtsVersion,
      "--local-bin-path", toolsBinPath.getPath,
      "install", toolName,
      "--progress-bar", "full",
      "--no-interleaved-output"
    )

    val result = runWithProgressIndicator(
      project,
      workDir = Some(VfsUtil.getUserHomeDir.getPath),
      arguments,
      HaskeletorBundle.message("progress.installing-tool.title", toolName),
      Some(progressIndicator)
    ).exists(handler => {

      val output = handler.runProcessWithProgressIndicator(progressIndicator)

      if (output.isCancelled) {
        handler.destroyProcess()
      }

      if (output.getExitCode != 0) {
        if (output.getStderr.nonEmpty) {
          HaskellNotificationGroup.logErrorBalloonEvent(project, output.getStderr)
        }
        if (output.getStdout.nonEmpty) {
          HaskellNotificationGroup.logErrorBalloonEvent(project, output.getStdout)
        }
      }
      output.getExitCode == 0 && !output.isCancelled && !output.isTimeout
    })

    result
  }

  def updateStackIndex(project: Project): Option[ProcessOutput] = {
    val arguments = Seq("update")
    run(project, arguments, -1, logOutput = true, notifyBalloonError = true, enableExtraArguments = false)
  }

  def buildProjectDependenciesInMessageView(project: Project): Option[Boolean] = {
    buildInMessageView(project, "Build project dependencies", Seq("--test", "--bench", "--no-run-tests", "--no-run-benchmarks", "--only-dependencies"))
  }

  private def ghcOptions(project: Project) = {
    if (HaskellProjectUtil.setNoDiagnosticsShowCaretFlag(project)) {
      Seq("--ghc-options", NoDiagnosticsShowCaretFlag)
    } else {
      Seq()
    }
  }

  def buildInBackground(project: Project, arguments: Seq[String]): Option[Boolean] = {
    run(project, Seq("build", "--fast") ++ arguments).map(_.getExitCode == 0)
  }

  def buildInMessageView(project: Project, description: String, arguments: Seq[String]): Option[Boolean] = {
    executeStackCommandInBuildView(project, description, Seq("build", "--fast", "--progress-bar", "full", "--no-interleaved-output") ++ arguments ++ ghcOptions(project))
  }

  // To prevent message window is not yet available
  private def waitForProjectIsInitialized(project: Project): WaitFor = {
    new WaitFor(5000, 1) {
      override def condition(): Boolean = {
        project.isInitialized
      }
    }
  }
}
