/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.external.execution

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.GeneralCommandLine.ParentEnvironmentType
import com.intellij.execution.process
import com.intellij.execution.process._
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.io.BaseOutputReader
import me.fornever.haskeletor.GlobalInfo
import me.fornever.haskeletor.core.notifications.HaskellNotificationGroup
import me.fornever.haskeletor.core.stack.OutputToProgressIndicator
import org.jetbrains.annotations.Nls

import java.nio.charset.Charset
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

object CommandLine {
  val DefaultTimeout: FiniteDuration = 60.seconds
  val DefaultNotifyBalloonError = false
  val DefaultIgnoreExitCode = false
  val DefaultLogOutput = false

  def run(project: Project, commandPath: String, arguments: Seq[String], timeoutInMillis: Long = DefaultTimeout.toMillis,
          notifyBalloonError: Boolean = DefaultNotifyBalloonError, ignoreExitCode: Boolean = DefaultIgnoreExitCode,
          logOutput: Boolean = DefaultLogOutput, charset: Option[Charset] = None): ProcessOutput = {
    run3(Some(project), project.getBasePath, commandPath, arguments, timeoutInMillis, notifyBalloonError, ignoreExitCode,
      logOutput, charset)
  }

  def runInWorkDir(project: Project, workDir: String, commandPath: String, arguments: Seq[String], timeoutInMillis: Long = DefaultTimeout.toMillis,
                   notifyBalloonError: Boolean = DefaultNotifyBalloonError, ignoreExitCode: Boolean = DefaultIgnoreExitCode,
                   logOutput: Boolean = DefaultLogOutput, charset: Option[Charset] = None): ProcessOutput = {
    run3(Some(project), workDir, commandPath, arguments, timeoutInMillis, notifyBalloonError, ignoreExitCode,
      logOutput, charset)
  }

  def runInHomeDir(commandPath: String, arguments: Seq[String], timeoutInMillis: Long = DefaultTimeout.toMillis,
                   notifyBalloonError: Boolean = DefaultNotifyBalloonError, ignoreExitCode: Boolean = DefaultIgnoreExitCode,
                   logOutput: Boolean = DefaultLogOutput, charset: Option[Charset] = None): ProcessOutput = {
    run3(None, VfsUtil.getUserHomeDir.getPath, commandPath, arguments, timeoutInMillis, notifyBalloonError, ignoreExitCode,
      logOutput, charset)
  }

  def runWithProgressIndicator(project: Project,
                               workDir: Option[String],
                               commandPath: String,
                               arguments: Seq[String],
                               @Nls title: String,
                               progressIndicator: Option[ProgressIndicator],
                               charset: Option[Charset] = None): CapturingProcessHandler = {
    val commandLine = createCommandLine(workDir.getOrElse(project.getBasePath), commandPath, arguments, charset)

    new CapturingProcessHandler(commandLine) {
      override protected def createProcessAdapter(processOutput: ProcessOutput): CapturingProcessAdapter = {
        progressIndicator match {
          case Some(pi) => new OutputToProgressIndicator(title, pi)
          case None => super.createProcessAdapter(processOutput)
        }
      }

      override def readerOptions(): BaseOutputReader.Options = {
        BaseOutputReader.Options.forMostlySilentProcess()
      }
    }
  }

  private def run3(project: Option[Project], workDir: String, commandPath: String, arguments: Seq[String], timeoutInMillis: Long = DefaultTimeout.toMillis,
                   notifyBalloonError: Boolean = DefaultNotifyBalloonError, ignoreExitCode: Boolean = DefaultIgnoreExitCode,
                   logOutput: Boolean = DefaultLogOutput, charset: Option[Charset] = None): ProcessOutput = {

    val commandLine = createCommandLine(workDir, commandPath, arguments, charset)

    if (!logOutput) {
      HaskellNotificationGroup.logInfoEvent(project, s"Executing: ${commandLine.getCommandLineString} ")
    }

    val processHandler = createProcessHandler(project, commandLine, logOutput)

    val processOutput = processHandler.map(_.runProcess(timeoutInMillis.toInt, true)).getOrElse(new process.ProcessOutput(-1))

    if (processOutput.isTimeout) {
      val message = s"Timeout while executing `${commandLine.getCommandLineString}`"
      if (notifyBalloonError) {
        HaskellNotificationGroup.logErrorBalloonEvent(project, message)
      } else {
        HaskellNotificationGroup.logErrorEvent(project, message)
      }
      processOutput
    } else if (!ignoreExitCode && processOutput.getExitCode != 0) {
      val errorMessage = createLogMessage(commandLine, processOutput)
      val message = s"Executing `${commandLine.getCommandLineString}` failed: $errorMessage"
      if (notifyBalloonError) HaskellNotificationGroup.logErrorBalloonEvent(project, message) else HaskellNotificationGroup.logErrorEvent(project, message)
      processOutput
    } else {
      processOutput
    }
  }

  def createCommandLine(workDir: String, commandPath: String, arguments: Seq[String], charset: Option[Charset] = None): GeneralCommandLine = {
    val commandLine = new GeneralCommandLine
    commandLine.withWorkDirectory(workDir)
    commandLine.setExePath(commandPath)
    commandLine.addParameters(arguments.asJava)
    commandLine.withParentEnvironmentType(ParentEnvironmentType.CONSOLE)
    commandLine.withEnvironment(GlobalInfo.pathVariables)
    charset.foreach(commandLine.setCharset)
    commandLine
  }

  private def createProcessHandler(project: Option[Project], cmd: GeneralCommandLine, logOutput: Boolean): Option[CapturingProcessHandler] = {
    try {
      if (logOutput) {
        Some(
          new CapturingProcessHandler(cmd) {
            override protected def createProcessAdapter(processOutput: ProcessOutput): CapturingProcessAdapter = new CapturingProcessToLog(project, cmd, processOutput)
          })
      } else {
        Some(new CapturingProcessHandler(cmd))
      }
    } catch {
      case e: ProcessNotCreatedException =>
        HaskellNotificationGroup.logErrorBalloonEvent(project, e.getMessage)
        None
    }
  }

  private def createLogMessage(cmd: GeneralCommandLine, processOutput: ProcessOutput): String = {
    s"${cmd.getCommandLineString}:  ${processOutput.getStdoutLines.asScala.mkString("\n")} \n ${processOutput.getStderrLines.asScala.mkString("\n")}"
  }
}

private class CapturingProcessToLog(val project: Option[Project], val cmd: GeneralCommandLine, val output: ProcessOutput) extends CapturingProcessAdapter(output) {

  override def onTextAvailable(event: ProcessEvent, outputType: Key[_]): Unit = {
    super.onTextAvailable(event, outputType)
    addToLog(event.getText)
  }

  private def addToLog(text: String): Unit = {
    val trimmedText = text.trim
    if (trimmedText.nonEmpty) {
      HaskellNotificationGroup.logInfoEvent(project, s"${cmd.getCommandLineString}:  $trimmedText")
    }
  }
}
