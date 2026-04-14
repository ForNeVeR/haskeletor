/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.external.execution

import com.intellij.build.BuildViewEvent.BuildStarted
import com.intellij.build.events.StartBuildEvent
import com.intellij.build.{BuildView, BuildViewManager, DefaultBuildDescriptor}
import com.intellij.compiler.impl._
import com.intellij.compiler.progress.CompilerTask
import com.intellij.concurrency.JobSchedulerImpl.getCPUCoresCount
import com.intellij.execution.ExecutionException
import com.intellij.execution.process._
import com.intellij.openapi.compiler.{CompileContext, CompileTask, CompilerMessageCategory}
import com.intellij.openapi.progress.{ProgressIndicator, ProgressManager}
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.WaitFor
import me.fornever.haskeletor.sdk.HaskellSdkType
import me.fornever.haskeletor.settings.HaskellSettingsState
import me.fornever.haskeletor.stackyaml.StackYamlComponent
import me.fornever.haskeletor.util.{HaskellFileUtil, HaskellProjectUtil}
import me.fornever.haskeletor.{HaskeletorBundle, HaskellNotificationGroup}
import org.jetbrains.annotations.Nls

import java.nio.charset.Charset
import java.util.concurrent.{LinkedBlockingDeque, TimeUnit}
import scala.concurrent.Future
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
    executeStackCommandInMessageView(project, description, Seq("build", "--fast", "--progress-bar", "full", "--no-interleaved-output") ++ arguments ++ ghcOptions(project))
  }

  def executeStackCommandInMessageView(project: Project, description: String, arguments: Seq[String]): Option[Boolean] = {
    HaskellSdkType.getStackPath(project).flatMap(stackPath => {
      executeInMessageView(project, description, stackPath, arguments)
    })
  }

  // To prevent message window is not yet available
  private def waitForProjectIsInitialized(project: Project): WaitFor = {
    new WaitFor(5000, 1) {
      override def condition(): Boolean = {
        project.isInitialized
      }
    }
  }

  def executeInMessageView(project: Project, description: String, commandPath: String, arguments: Seq[String]): Future[Option[Boolean]] = {
    waitForProjectIsInitialized(project)

    val workingDirectory = project.getBasePath
    val cmd = CommandLine.createCommandLine(
      workingDirectory,
      commandPath,
      Seq(
        "--terminal",
        "--color", "never"
      ) ++ HaskellSettingsState.getExtraStackArguments ++ arguments)
    (try {
      Option(cmd.createProcess())
    } catch {
      case e: ExecutionException =>
        HaskellNotificationGroup.logErrorBalloonEvent(project, s"Error while starting `${cmd.getCommandLineString}`: ${e.getMessage}")
        None
    }).map(process => {

      val handler = new BaseOSProcessHandler(process, cmd.getCommandLineString, Charset.defaultCharset())

      val buildId = cmd.getCommandLineString // TODO: Let's find a better id.
      val buildDescriptor = new DefaultBuildDescriptor(
        cmd.getCommandLineString,
        description,
        workingDirectory,
        System.currentTimeMillis
      )
      val buildView = new BuildView(project, buildDescriptor, null, project.getService(classOf[BuildViewManager]))
      val adapter = new MessageViewProcessAdapter(buildView)
      handler.addProcessListener(adapter)
      handler.startNotify()
      buildView.onEvent(buildId, StartBuildEvent.builder("Build started", buildDescriptor).build())

      adapter.addLastMessage()
      handler.getProcess.onExit()
      handler.getExitCode == 0 || handler.getExitCode == null

      handler.getExitCode == 0 || handler.getExitCode == null

      val compileTask = new CompileTask {

        def execute(compileContext: CompileContext): Boolean = {

        }
      }

      val compileContext = new CompileContextImpl(project, compilerTask, new ProjectCompileScope(project), false, false)

      val compileResult = new LinkedBlockingDeque[Boolean](1)

      compilerTask.start(() => {
        compileResult.put(compileTask.execute(compileContext))
      }, null)

      val result = compileResult.poll(30, TimeUnit.MINUTES) // Wait max half an hour
      val exitStatus = if (result) ExitStatus.SUCCESS else ExitStatus.ERRORS
      compilerTask.setEndCompilationStamp(exitStatus, System.currentTimeMillis)
      result
    })
  }


  private class MessageViewProcessAdapter(val compileContext: CompileContext) extends ProcessAdapter() {

    private val ansiEscapeDecoder = new AnsiEscapeDecoder()
    private val previousMessageLines = new LinkedBlockingDeque[String]

    @volatile
    private var globalError = false

    override def onTextAvailable(event: ProcessEvent, outputType: Key[_]): Unit = {
      val text = AnsiDecoder.decodeAnsiCommandsToString(event.getText, outputType, ansiEscapeDecoder)
      addToMessageView(text, outputType)
    }

    def addLastMessage(): Unit = {
      if (!previousMessageLines.isEmpty) {
        addMessage()
      }
    }

    private def addToMessageView(text: String, outputType: Key[_]): Unit = {
      if (text.trim.nonEmpty) {
        if (outputType == ProcessOutputTypes.STDERR) {
          if (text.startsWith("Error:") && text.trim.endsWith(":") || text.startsWith("Unable to parse") || text.startsWith("Error parsing")) {
            globalError = true // To get also all lines after this line indicated as error AND in order
          }

          // End of sentence which was over multiple lines
          if (!previousMessageLines.isEmpty && !text.startsWith("  ")) {
            addMessage()
          }

          previousMessageLines.add(text)
        } else if (text.startsWith("Warning:")) {
          compileContext.addMessage(CompilerMessageCategory.WARNING, text, null, -1, -1)
        } else if (text.startsWith("Error:")) {
          compileContext.addMessage(CompilerMessageCategory.ERROR, text, null, -1, -1)
        } else {
          compileContext.addMessage(CompilerMessageCategory.INFORMATION, text, null, -1, -1)
        }
      }
    }


    private def addMessage(): Unit = {
      val errorMessageLine = previousMessageLines.iterator().asScala.mkString(" ")
      val compilationProblem = HaskellCompilationResultHelper.parseErrorLine(errorMessageLine.replaceAll("\n", " "))
      compilationProblem match {
        case Some(p@CompilationProblem(filePath, lineNr, columnNr, message)) if p.isWarning =>
          compileContext.addMessage(CompilerMessageCategory.WARNING, message, HaskellFileUtil.getUrlByPath(filePath), lineNr, columnNr)
        case Some(CompilationProblem(filePath, lineNr, columnNr, message)) =>
          compileContext.addMessage(CompilerMessageCategory.ERROR, message, HaskellFileUtil.getUrlByPath(filePath), lineNr, columnNr)
        case _ =>
          val compilerMessageCategory =
            if (globalError || errorMessageLine.contains("ExitFailure") || errorMessageLine.startsWith("Error:")) {
              CompilerMessageCategory.ERROR
            } else if (errorMessageLine.startsWith("Warning:")) {
              CompilerMessageCategory.WARNING
            } else {
              CompilerMessageCategory.INFORMATION
            }
          compileContext.addMessage(compilerMessageCategory, errorMessageLine, null, -1, -1)
      }
      previousMessageLines.clear()
    }
  }
}
