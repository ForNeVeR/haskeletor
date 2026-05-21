/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.language

import com.intellij.formatting.service.{AsyncDocumentFormattingService, AsyncFormattingRequest, FormattingService}
import com.intellij.psi.PsiFile
import me.fornever.haskeletor.action.OrmoluReformatAction
import me.fornever.haskeletor.core.HaskeletorBundle
import me.fornever.haskeletor.util.HaskellFileUtil

import java.util
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

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
          future <- OrmoluReformatAction.reformatFile(formattingRequest.getContext.getProject, ioFile.toPath)
        } yield future

        maybeFuture match {
          case Some(future) =>
            runningFuture.set(future)
            future.whenComplete((formattedText, throwable) => {
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
            })
          case None =>
            formattingRequest.onTextReady(null)
        }
      }

      override def cancel(): Boolean = Option(runningFuture.get()).exists(_.cancel(true))

      override def isRunUnderProgress: Boolean = true
  }
}
