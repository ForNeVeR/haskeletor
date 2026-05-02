/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.util

import com.intellij.openapi.project.Project
import me.fornever.haskeletor.core.notifications.HaskellNotificationGroup

import java.util.concurrent.{Future, TimeUnit, TimeoutException}
import scala.concurrent.Await
import scala.concurrent.duration._

object FutureUtil {

  def waitForValue[T](project: Project, future: Future[T], actionDescription: String, timeoutInSeconds: Int = 5): Option[T] = {
    try {
      Option(future.get(timeoutInSeconds, TimeUnit.SECONDS))
    } catch {
      case _: TimeoutException =>
        HaskellNotificationGroup.logInfoEvent(project, s"Timeout while $actionDescription")
        None
    }
  }

  def waitForValue[T](project: Project, future: scala.concurrent.Future[T], actionDescription: String, timeoutInSeconds: Int = 5): Option[T] = {
    try {
      Option(Await.result(future, timeoutInSeconds.seconds))
    } catch {
      case _: TimeoutException =>
        HaskellNotificationGroup.logInfoEvent(project, s"Timeout while $actionDescription")
        None
    }
  }
}
