/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.util

import com.intellij.openapi.application.{ApplicationManager, ReadAction}
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.progress.util.ProgressIndicatorBase
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.AppExecutorUtil
import me.fornever.haskeletor.external.component.NoInfo

import java.util.concurrent.TimeUnit
import scala.concurrent.TimeoutException

object ApplicationUtil {

  def isBlockingReadAccessAllowed: Boolean = {
    ApplicationManager.getApplication.isReadAccessAllowed
  }

  def runReadAction[T](f: => T, project: Option[Project] = None): T = {
    if (isBlockingReadAccessAllowed) {
      f
    } else {
      val progressIndicator = Option(ProgressIndicatorProvider.getGlobalProgressIndicator).getOrElse(new ProgressIndicatorBase(false, false))
      val readAction = ReadAction.nonBlocking(ScalaUtil.callable(f))
      readAction.wrapProgress(progressIndicator)
      project.foreach(readAction.expireWith)
      readAction.submit(AppExecutorUtil.getAppExecutorService).get(5, TimeUnit.SECONDS)
    }
  }

  def runReadActionWithFileAccess[A](project: Project, f: => A, actionDescription: => String): Either[NoInfo, A] = {
    if (isBlockingReadAccessAllowed) {
      Right(f)
    } else {
      val progressIndicator = Option(ProgressIndicatorProvider.getGlobalProgressIndicator)
      val readAction = ReadAction.nonBlocking(ScalaUtil.callable(f)).inSmartMode(project)
      progressIndicator.foreach(readAction.wrapProgress)
      readAction.expireWith(project)
      try {
        Option(readAction.submit(AppExecutorUtil.getAppExecutorService).get(5, TimeUnit.SECONDS)) match {
          case Some(x) => Right(x)
          case None => Left(IndexNotReady)
        }
      } catch {
        case _: TimeoutException =>
          HaskellNotificationGroup.logInfoEvent(project, s"Timeout in readActionWithFileAccess while $actionDescription")
          Left(ReadActionTimeout(actionDescription))
      }
    }
  }
}

