/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.language

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.formatting.service.{AsyncDocumentFormattingService, AsyncFormattingRequest, FormattingService}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import me.fornever.haskeletor.core.HaskeletorBundle
import me.fornever.haskeletor.external.component.StackProjectManager
import me.fornever.haskeletor.settings.HTool
import me.fornever.haskeletor.util.{HaskellFileUtil, ScalaUtil}

import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import scala.io.Source

final class HaskellFormattingService extends AsyncDocumentFormattingService {

  override def getFeatures: util.Set[FormattingService.Feature] = util.Collections.emptySet()

  override def canFormat(file: PsiFile): Boolean = HaskellFileUtil.isHaskellFile(file)

  override def getNotificationGroupId: String = "HaskellBalloon"

  override def getName: String = "Ormolu"

  override protected def createFormattingTask(formattingRequest: AsyncFormattingRequest): AsyncDocumentFormattingService.FormattingTask =
    new AsyncDocumentFormattingService.FormattingTask {
      private val runningFuture = new AtomicReference[CompletableFuture[String]]()

      override def run(): Unit = {
        val maybeFuture = for {
          ioFile <- Option(formattingRequest.getIOFile)
          future <- reformatFile(formattingRequest.getContext.getProject, ioFile.toPath, formattingRequest)
        } yield future

        maybeFuture match {
          case Some(future) =>
            runningFuture.set(future)
            future.whenComplete((formattedText, throwable) => {
              if (future.isCancelled) {
                // Suppress error notification on user-initiated cancellation
                () // No-op
              } else {
                Option(formattedText) match {
                  case Some(text) => formattingRequest.onTextReady(HaskellFileUtil.normalizeLineEndings(text))
                  case None =>
                    val errorMessage =
                      Option(throwable).flatMap { error =>
                        Option(error.getLocalizedMessage)
                          .orElse(Option(error.getMessage))
                      }.getOrElse(HaskeletorBundle.message("formatting.error-notification.unknown-error"))
                    formattingRequest.onError(
                      HaskeletorBundle.message("formatting.error-notification.title"),
                      errorMessage
                    )
                }
              }
            })
          case None =>
            formattingRequest.onTextReady(null)
        }
      }

      override def cancel(): Boolean = Option(runningFuture.get()).exists(_.cancel(true))

      override def isRunUnderProgress: Boolean = true
    }

  private def reformatFile(project: Project, file: Path, formattingRequest: AsyncFormattingRequest): Option[CompletableFuture[String]] = {
    StackProjectManager.isOrmoluAvailable(project).map { ormoluPath =>
      val commandLine = new GeneralCommandLine(
        ormoluPath,
        file.toString
      )

      // Set working directory from project
      StackProjectManager.findWorkingDirectory(project).foreach { workDir =>
        commandLine.withWorkDirectory(workDir.toFile)
      }

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
              readStream(process.getInputStream, formattingRequest)
            })
            val stderrFuture = ApplicationManager.getApplication.executeOnPooledThread(ScalaUtil.callable[String] {
              readStream(process.getErrorStream, formattingRequest)
            })

            val exitCode = process.waitFor()
            val stdout = stdoutFuture.get()
            val stderr = stderrFuture.get()

            if (exitCode == 0) {
              future.complete(stdout)
            } else {
              logger.error(
                s"Ormolu reformat process failed for `$file` with exit code $exitCode.\nstdout:\n$stdout\nstderr:\n$stderr"
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

  private val logger = Logger.getInstance(getClass)

  private def readStream(stream: InputStream, formattingRequest: AsyncFormattingRequest): String = {
    val charset = Option(formattingRequest.getIOFile)
      .flatMap(f => HaskellFileUtil.findVirtualFile(formattingRequest.getContext.getProject, f.getAbsolutePath))
      .flatMap(vf => Option(vf.getCharset))
      .getOrElse(StandardCharsets.UTF_8)
    val source = Source.fromInputStream(stream, charset.name())
    try source.mkString
    finally source.close()
  }
}
