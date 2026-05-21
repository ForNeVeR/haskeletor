/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.action

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import me.fornever.haskeletor.core.notifications.HaskellNotificationGroup
import me.fornever.haskeletor.external.component.StackProjectManager
import me.fornever.haskeletor.external.execution.CommandLine
import me.fornever.haskeletor.settings.HTool
import me.fornever.haskeletor.util._

import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import scala.io.Source

class OrmoluReformatAction extends AnAction {

  override def update(actionEvent: AnActionEvent): Unit = {
    HaskellEditorUtil.enableExternalAction(actionEvent, (project: Project) => StackProjectManager.isOrmoluAvailable(project).isDefined)
  }

  override def actionPerformed(actionEvent: AnActionEvent): Unit = {
    ActionUtil.findActionContext(actionEvent).foreach { actionContext =>
      val psiFile = actionContext.psiFile
      OrmoluReformatAction.reformat(psiFile)
    }
  }
}

object OrmoluReformatAction {

  def reformatFile(project: Project, file: Path): Option[CompletableFuture[String]] = {
    StackProjectManager.isOrmoluAvailable(project).map { ormoluPath =>
      val commandLine = new GeneralCommandLine(
        ormoluPath,
        file.toString
      )

      val processRef = new AtomicReference[Process]()
      val cancelled = new AtomicBoolean(false)

      val future = new CompletableFuture[String]() {
        override def cancel(mayInterruptIfRunning: Boolean): Boolean = {
          val wasCancelled = super.cancel(mayInterruptIfRunning)
          if (wasCancelled) {
            cancelled.set(true)
            Option(processRef.get()).foreach { process =>
              process.destroy()
              if (process.isAlive) {
                process.destroyForcibly()
              }
            }
          }
          wasCancelled
        }
      }

      ApplicationManager.getApplication.executeOnPooledThread(ScalaUtil.runnable {
        try {
          val process = commandLine.createProcess()
          processRef.set(process)

          if (future.isCancelled || cancelled.get()) {
            process.destroy()
            if (process.isAlive) {
              process.destroyForcibly()
            }
          } else {
            val stdoutFuture = ApplicationManager.getApplication.executeOnPooledThread(ScalaUtil.callable[String] {
              readStream(process.getInputStream)
            })
            val stderrFuture = ApplicationManager.getApplication.executeOnPooledThread(ScalaUtil.callable[String] {
              readStream(process.getErrorStream)
            })

            val exitCode = process.waitFor()
            val stdout = stdoutFuture.get()
            val stderr = stderrFuture.get()

            if (exitCode == 0) {
              future.complete(stdout)
            } else {
              logger.error(
                s"Ormolu reformat process failed for `${file}` with exit code $exitCode.\nstdout:\n$stdout\nstderr:\n$stderr"
              )
              val truncatedStderr = stderr.take(1024)
              future.completeExceptionally(
                new RuntimeException(
                  s"Error while reformatting by `${HTool.Ormolu.name}`. Exit code: $exitCode. Error: $truncatedStderr"
                )
              )
            }
          }
        } catch {
          case e: Throwable =>
            if (!future.isCancelled && !cancelled.get()) {
              future.completeExceptionally(e)
            }
        }
      })

      future
    }
  }

  def reformat(psiFile: PsiFile): Boolean = {
    val project = psiFile.getProject
    StackProjectManager.isOrmoluAvailable(project) match {
      case Some(ormoluPath) =>
        HaskellFileUtil.saveFile(psiFile)

        HaskellFileUtil.getAbsolutePath(psiFile) match {
          case Some(path) =>
            val processOutputFuture = ApplicationManager.getApplication.executeOnPooledThread(ScalaUtil.callable[ProcessOutput] {
              val fileCharset = HaskellFileUtil.getCharset(psiFile)
              CommandLine.run(project, Path.of(ormoluPath), Seq(path), charset = fileCharset)
            })

            FutureUtil.waitForValue(project, processOutputFuture, s"reformatting by ${HTool.Ormolu.name}") match {
              case None => false
              case Some(processOutput) =>
                if (processOutput.getStderrLines.isEmpty) {
                  HaskellFileUtil.saveFileWithNewContent(psiFile, processOutput.getStdout)
                  true
                } else {
                  HaskellNotificationGroup.logErrorBalloonEvent(project, s"Error while reformatting by `${HTool.Ormolu.name}`. Error: ${processOutput.getStderr}")
                  false
                }
            }
          case None =>
            HaskellNotificationGroup.logWarningBalloonEvent(psiFile.getProject, s"Can not reformat file because could not determine path for file `${psiFile.getName}`. File exists only in memory")
            false
        }
      case None =>
        HaskellNotificationGroup.logWarningBalloonEvent(psiFile.getProject, s"Can not reformat file because `${HTool.Ormolu.name}` is not (yet) available")
        false
    }
  }

  def versionInfo(project: Project): String = {
    StackProjectManager.isOrmoluAvailable(project) match {
      case Some(ormoluPath) => CommandLine.run(project, Path.of(ormoluPath), Seq("--version")).getStdout
      case None => "-"
    }
  }

  private val logger = Logger.getInstance(getClass)

  private def readStream(stream: InputStream): String = {
    val source = Source.fromInputStream(stream, StandardCharsets.UTF_8.name())
    try source.mkString
    finally source.close()
  }
}
