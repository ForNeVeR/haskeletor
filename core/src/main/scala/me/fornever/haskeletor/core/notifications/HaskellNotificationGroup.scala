/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.core.notifications

import com.intellij.notification._
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.MessageType._
import com.intellij.openapi.vfs.LocalFileSystem

import javax.swing.event.HyperlinkEvent

object HaskellNotificationGroup {

  private val LogOnlyGroup = new NotificationGroup("Haskell Log", NotificationDisplayType.NONE, false)
  private val WarningGroup = new NotificationGroup("Haskell Warning", NotificationDisplayType.NONE, true)
  private val BalloonGroup = new NotificationGroup("Haskell Balloon", NotificationDisplayType.BALLOON, true)

  def logErrorEvent(project: Option[Project], message: String): Unit = {
    logEvent(project, message, ERROR, LogOnlyGroup.createNotification)
  }

  def logErrorEvent(project: Project, message: String): Unit = {
    logEvent(Option(project), message, ERROR, LogOnlyGroup.createNotification)
  }

  def logErrorEvent(message: String): Unit = {
    logEvent(None, message, ERROR, LogOnlyGroup.createNotification)
  }

  def logWarningEvent(project: Project, message: String): Unit = {
    logEvent(Option(project), message, WARNING, LogOnlyGroup.createNotification)
  }

  def warningEvent(project: Project, message: String): Unit = {
    logEvent(Option(project), message, WARNING, WarningGroup.createNotification)
  }

  def logWarningEvent(message: String): Unit = {
    logEvent(None, message, WARNING, LogOnlyGroup.createNotification)
  }

  def logInfoEvent(project: Option[Project], message: String): Unit = {
    logEvent(project, message, INFO, LogOnlyGroup.createNotification)
  }

  def logInfoEvent(project: Project, message: String): Unit = {
    logEvent(Option(project), message, INFO, LogOnlyGroup.createNotification)
  }

  def logInfoEvent(message: String): Unit = {
    logEvent(None, message, INFO, LogOnlyGroup.createNotification)
  }

  def logErrorBalloonEvent(project: Option[Project], message: String): Unit = {
    balloonEvent(project, message, ERROR)
  }

  def logErrorBalloonEvent(project: Project, message: String): Unit = {
    balloonEvent(Option(project), message, ERROR)
  }

  def logErrorBalloonEvent(project: Project, message: String, listener: NotificationListener): Unit = {
    BalloonGroup.createNotification("", message, ERROR.toNotificationType, listener).notify(project)
  }

  def logErrorBalloonEvent(message: String): Unit = {
    balloonEvent(None, message, ERROR)
  }

  def logWarningBalloonEvent(project: Option[Project], message: String): Unit = {
    balloonEvent(project, message, WARNING)
  }

  def logWarningBalloonEvent(project: Project, message: String): Unit = {
    balloonEvent(Option(project), message, WARNING)
  }

  def logWarningBalloonEvent(message: String): Unit = {
    balloonEvent(None, message, WARNING)
  }

  def logInfoBalloonEvent(project: Project, message: String): Unit = {
    balloonEvent(Option(project), message, INFO)
  }

  def logInfoBalloonEvent(message: String): Unit = {
    balloonEvent(None, message, INFO)
  }

  def logInfoBalloonEvent(project: Project, message: String, listener: NotificationListener): Unit = {
    BalloonGroup.createNotification("", message, INFO.toNotificationType, listener).notify(project)
  }

  def logErrorBalloonEventWithLink(project: Project, filePath: String, errorMessage: String, lineNr: Int, columnNr: Int): Unit = {
    Option(LocalFileSystem.getInstance().findFileByPath(filePath)) match {
      case Some(file) => HaskellNotificationGroup.logErrorBalloonEvent(project, s"$errorMessage at <a href='#'>$filePath:$lineNr:$columnNr</a>",
        (_: Notification, _: HyperlinkEvent) => {
          new OpenFileDescriptor(project, file, lineNr - 1, columnNr - 1).navigate(true)
        }
      )
      case _ => HaskellNotificationGroup.logErrorBalloonEvent(project, errorMessage)
    }
  }

  private def logEvent(project: Option[Project], message: String, messageType: MessageType, notification: (String, MessageType) => Notification): Unit = {
    log(project, message, messageType, notification)
  }

  private def balloonEvent(project: Option[Project], message: String, messageType: MessageType): Unit = {
    log(project, message, messageType, BalloonGroup.createNotification)
  }

  private def log(project: Option[Project], message: String, messageType: MessageType, notification: (String, MessageType) => Notification): Unit = {
    project match {
      case Some(p) if !p.isDisposed && p.isOpen => notification(message, messageType).notify(p)
      case None => notification(message, messageType).notify(null)
      case _ => ()
    }
  }
}
