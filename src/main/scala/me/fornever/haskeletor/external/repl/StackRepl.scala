/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.external.repl

import com.intellij.openapi.project.Project
import me.fornever.haskeletor.GlobalInfo
import me.fornever.haskeletor.core.notifications.HaskellNotificationGroup
import me.fornever.haskeletor.core.util.StringUtil
import me.fornever.haskeletor.external.execution.StackCommandLine
import me.fornever.haskeletor.external.repl.StackRepl.{BenchmarkType, ExeType, StackReplOutput, TestSuiteType}
import me.fornever.haskeletor.external.repl.StackReplsManager.ProjectReplTargets
import me.fornever.haskeletor.sdk.HaskellSdkType
import me.fornever.haskeletor.util.{HaskellEditorUtil, HaskellFileUtil, HaskellProjectUtil}

import java.io._
import java.util.concurrent.LinkedBlockingQueue
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.io._
import scala.jdk.CollectionConverters._
import scala.sys.process._

abstract class StackRepl(project: Project, projectReplTargets: Option[ProjectReplTargets], extraReplOptions: Seq[String] = Seq(), replTimeout: Int, ghcOptions: Seq[String] = Seq()) {

  private val stanzaType = projectReplTargets.map(_.stanzaType)

  private object GhciCommand {

    trait Command

    case object Load extends Command

    case object Browse extends Command

    case object Set extends Command

    case object Module extends Command

    case object ShowModules extends Command

    case object OtherCommand extends Command

  }

  private final val LineSeparator = '\n'

  @volatile
  private[external] var available = false

  @volatile
  private[external] var starting = false

  private[this] val outputStreamQueue = new LinkedBlockingQueue[OutputStream]

  private[this] val stdoutQueue = new LinkedBlockingQueue[String]

  private[this] val stderrQueue = new LinkedBlockingQueue[String]

  private final val LoadTimeout = 60.seconds

  private final val DefaultTimeout = replTimeout.seconds

  private final val EndOfOutputIndicator = "^IntellijHaskell^"

  private final val DelayBetweenReadsInMillis = 1

  private final val ExitCommand = ":q"

  private final val CanNotSatisfyErrorMessageIndicator = "<command line>: cannot satisfy -package"

  protected def clearLoadedModules(): Unit

  def getComponentName: String = projectReplTargets.map(_.targetsName).map(name => "project-stack-repl-" + name).getOrElse("global-stack-repl")

  def isGlobalRepl: Boolean = projectReplTargets.isEmpty

  private val stdoutResult = new ArrayBuffer[String]
  private val stderrResult = new ArrayBuffer[String]


  // TODO: command should be a GhciCommand instead of a String
  protected def execute(command: String, forceExecute: Boolean = false): Option[StackReplOutput] = {

    if ((!available || starting) && !forceExecute) {
      HaskellEditorUtil.showStatusBarMessage(project, s"[$getComponentName] Haskell support is only available when REPL is running")
      None
    } else {

      def init(): Unit = {
        stdoutQueue.clear()
        stderrQueue.clear()

        stdoutResult.clear()
        stderrResult.clear()
      }

      def logOutput(): Unit = {
        if (stdoutResult.nonEmpty) logInfo("stdout: " + stdoutResult.mkString("\n"))
        if (stderrResult.nonEmpty) {
          val stderrMessage = "stderr: " + stderrResult.mkString("\n")
          logError(stderrMessage)
        }
      }

      def drainQueues(): Unit = {
        stdoutQueue.drainTo(stdoutResult.asJava)
        stderrQueue.drainTo(stderrResult.asJava)
        ()
      }

      try {
        Option(outputStreamQueue.peek()) match {
          case Some(output) =>
            init()

            val ghciCommand = command match {
              case c if c.startsWith(":browse!") => GhciCommand.Browse
              case c if c.startsWith(":load") | c.startsWith(":reload") => GhciCommand.Load
              case c if c.startsWith(":module") => GhciCommand.Module
              case c if c.startsWith(":set") => GhciCommand.Set
              case c if c == ":show modules" => GhciCommand.ShowModules
              case _ => GhciCommand.OtherCommand
            }

            def outputContainsEndOfOutputIndicator: Boolean = {
              stdoutResult.lastOption.exists(_.contains(EndOfOutputIndicator))
            }

            def hasReachedEndOfOutput: Boolean = {
              if (command == ExitCommand) {
                stdoutResult.lastOption.exists(_.startsWith("Leaving GHCi"))
              } else {
                outputContainsEndOfOutputIndicator && (ghciCommand == GhciCommand.ShowModules || ghciCommand == GhciCommand.Module || ghciCommand == GhciCommand.Set || stdoutResult.length > 1 || stderrResult.nonEmpty)
              }
            }

            output.write(command.getBytes)
            output.write(LineSeparator)
            output.flush()

            val timeout = if (ghciCommand == GhciCommand.Load || ghciCommand == GhciCommand.Browse) LoadTimeout else DefaultTimeout

            val deadline = timeout.fromNow
            while (deadline.hasTimeLeft() && !hasReachedEndOfOutput && !project.isDisposed) {
              drainQueues()

              // We have to wait...
              Thread.sleep(DelayBetweenReadsInMillis)
            }

            if (deadline.hasTimeLeft()) {
              logInfo(s"Command $command took + ${(timeout - deadline.timeLeft).toMillis} ms")
              val stdout = convertOutputToOneMessagePerLine(project, removePrompt(stdoutResult.toSeq))
              val stderr = convertOutputToOneMessagePerLine(project, stderrResult.toSeq)

              logInfo("REPL output:\n" + stdout.mkString("\n") + "\n" + stderr.mkString("\n"))

              Some(StackReplOutput(stdout, stderr))
            } else {
              drainQueues()
              logError(s"No result from REPL within $timeout. Command was: $command")
              exit(forceExit = true)
              None
            }
          case None =>
            logError("Can't write to REPL. Check if your Stack project environment is working okay")
            None
        }
      }
      catch {
        case e: Exception =>
          logError(s"Error in communication with REPL: ${e.getMessage}. Check if your Haskell/Stack environment is working okay. Command was: `$command`")
          drainQueues()
          logOutput()
          exit()
          None
      }
    }
  }


  def start(): Unit = synchronized {

    def writeOutputToLog(): Unit = {
      if (!stdoutQueue.isEmpty) {
        logInfo(stdoutQueue.asScala.mkString("\n"))
      }

      if (!stderrQueue.isEmpty) {
        stderrQueue.asScala.foreach(l => {
          if (l.startsWith("Configuring GHCi with") || l.startsWith("The following GHC options are incompatible with GHCi")) {
            logInfo(l)
          } else {
            logError(l)
          }
        })
      }
    }

    if (available || starting) {
      logInfo("REPL can't be started because it's already starting / running")
    } else {
      starting = true
      clearLoadedModules()

      HaskellSdkType.getStackPath(project).foreach(stackPath => {
        try {
          val extraOptions = if (stanzaType.contains(TestSuiteType)) {
            extraReplOptions ++ Seq("--test")
          } else if (stanzaType.contains(BenchmarkType)) {
            extraReplOptions ++ Seq("--bench")
          } else if (stanzaType.contains(ExeType)) {
            extraReplOptions ++ Seq("--no-load")
          } else {
            extraReplOptions
          }

          val replGhciOptionsFilePath = createGhciOptionsFile.getAbsolutePath
          val command = (Seq(stackPath, "repl") ++
            projectReplTargets.map(_.targetsName).toSeq ++
            Seq("--no-build", "--ghci-options", s"-ghci-script=$replGhciOptionsFilePath", "--silent", "--ghc-options", s""""-v1 ${ghcOptions.mkString(" ")}"""") ++ extraOptions).mkString(" ")

          logInfo(s"REPL will be started with command: $command")

          val processBuilder = Process(command, new File(project.getBasePath), GlobalInfo.pathVariables.asScala.toSeq: _*)

          stdoutQueue.clear()
          stderrQueue.clear()

          val process = processBuilder.run(
            new ProcessIO(
              in => outputStreamQueue.put(in),
              (out: InputStream) => Source.fromInputStream(out).getLines().foreach(stdoutQueue.add),
              (err: InputStream) => Source.fromInputStream(err).getLines().foreach(stderrQueue.add)
            ))

          def isStarted = {
            process.isAlive() && stdoutQueue.toArray(Array[String]()).lastOption.exists(_.contains(EndOfOutputIndicator))
          }

          def hasDependencyError = {
            stderrQueue.asScala.exists(_.startsWith(CanNotSatisfyErrorMessageIndicator))
          }

          val deadline = DefaultTimeout.fromNow
          while (process.isAlive() && deadline.hasTimeLeft() && !isStarted && !hasDependencyError && !project.isDisposed) {
            // We have to wait till REPL is started
            Thread.sleep(DelayBetweenReadsInMillis)
          }

          if (isStarted && !hasDependencyError) {
            if (stanzaType.isDefined) {
              execute(":set +c", forceExecute = true)
              execute(":set -fdefer-type-errors", forceExecute = true)
              execute(":set -fshow-loaded-modules", forceExecute = true)
              execute(":set -fno-max-valid-substitutions", forceExecute = true)
              if (HaskellProjectUtil.setNoDiagnosticsShowCaretFlag(project)) {
                execute(s":set ${StackCommandLine.NoDiagnosticsShowCaretFlag}", forceExecute = true)
              }
            }
            logInfo("REPL is started")
            available = true
          } else if (hasDependencyError) {
            val target = projectReplTargets.map(_.targetsName).getOrElse("-")
            val error = stderrQueue.asScala.find(_.startsWith(CanNotSatisfyErrorMessageIndicator)).map(_.replace("<command line>:", "").trim).getOrElse("a dependency failed to build")
            val message = s"REPL couldn't be started for target `$target` due to: $error"
            logInfo(message)
            HaskellNotificationGroup.logWarningBalloonEvent(project, message)
            closeResources()
          } else {
            logError(s"REPL couldn't be started within $DefaultTimeout")
            writeOutputToLog()
            closeResources()
          }
        }
        catch {
          case e: Exception =>
            logError(s"Couldn't start REPL. Error message ${e.getMessage}")
            writeOutputToLog()
            exit(forceExit = true)
        }
        finally {
          starting = false
        }
      })
    }
  }

  def exit(forceExit: Boolean = false): Unit = synchronized {
    if (!available && !forceExit) {
      logInfo("REPL couldn't be stopped because it's already stopped")
    } else {
      try {
        available = false
        execute(ExitCommand, forceExecute = true)
      }
      catch {
        case e: Exception =>
          logError(s"Error while shutting down REPL for project ${project.getName}. Error message: ${e.getMessage}")
      } finally {
        closeResources()
      }
      logInfo("REPL is stopped")
    }
  }

  private def createGhciOptionsFile: File = {
    // global-repl.ghci was used so can still be in cache directory
    val ghciOptionsFile = new File(GlobalInfo.getHaskeletorDirectory, "repl.ghci")

    if (!ghciOptionsFile.exists()) {
      ghciOptionsFile.createNewFile()
      ghciOptionsFile.setWritable(true, true)
      HaskellFileUtil.removeGroupWritePermission(ghciOptionsFile)

      val writer = new BufferedWriter(new FileWriter(ghciOptionsFile))
      try {
        writer.write(s""":set prompt "$EndOfOutputIndicator\\n"""")
      } finally {
        writer.close()
      }
    }
    ghciOptionsFile
  }

  private def closeResources(): Unit = {
    try {
      closeResource(stdin)
      closeResource(stdout)
      closeResource(stderr)
    } finally {
      if (!outputStreamQueue.isEmpty) {
        outputStreamQueue.clear()
      }
    }
  }

  private def closeResource(closeable: Closeable): Unit = {
    try {
      if (closeable != null) {
        closeable.close()
      }
    } catch {
      case _: IOException => ()
    }
  }

  def restart(forceExit: Boolean = false): Unit

  private def logError(message: String): Unit = {
    HaskellNotificationGroup.logErrorBalloonEvent(project, s"[$getComponentName] $message")
  }

  private def logInfo(message: String): Unit = {
    HaskellNotificationGroup.logInfoEvent(project, s"[$getComponentName] $message")
  }

  private def removePrompt(output: Seq[String]): Seq[String] = {
    if (output.lastOption.exists(_.trim == EndOfOutputIndicator)) {
      output.init
    } else {
      output
    }
  }

  // Loading file in GHCi with `set +c` produces duplicate error/warning messages
  private def convertOutputToOneMessagePerLine(project: Project, output: Seq[String]): Seq[String] = {
    StringUtil.joinIndentedLines(project, output.filterNot(_.isEmpty)).distinct
  }
}

object StackRepl {

  case class StackReplOutput(stdoutLines: Seq[String] = Seq(), stderrLines: Seq[String] = Seq())

  sealed trait StanzaType

  case object LibType extends StanzaType

  case object ExeType extends StanzaType

  case object TestSuiteType extends StanzaType

  case object BenchmarkType extends StanzaType

}
