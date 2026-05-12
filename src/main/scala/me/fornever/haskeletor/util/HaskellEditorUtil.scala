/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.util

import com.intellij.codeInsight.hint.{HintManager, HintManagerImpl, HintUtil}
import com.intellij.openapi.actionSystem.{AnActionEvent, CommonDataKeys}
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.util.NlsContexts.HintText
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.LightweightHint
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.UIUtil.FontSize
import me.fornever.haskeletor.HaskellFile

import java.awt.event.{MouseEvent, MouseMotionAdapter}
import scala.jdk.CollectionConverters._

object HaskellEditorUtil {

  final val HaskellSupportIsNotAvailableWhileInitializingText = "Haskell support is not available while project is initializing"

  def enableExternalAction(actionEvent: AnActionEvent, enableCondition: Project => Boolean): Unit = {
    Option(actionEvent.getProject) match {
      case Some(project) if HaskellProjectUtil.isHaskellProject(project) =>
        actionEvent.getPresentation.setVisible(true)
        actionEvent.getPresentation.setEnabled(HaskellProjectUtil.isHaskellProject(project) && enableCondition(project))
      case _ => actionEvent.getPresentation.setEnabledAndVisible(false)
    }
  }

  def enableAction(onlyForSourceFile: Boolean, actionEvent: AnActionEvent): Unit = {
    val presentation = actionEvent.getPresentation

    def enable(): Unit = {
      presentation.setEnabled(true)
      presentation.setVisible(true)
    }

    def disable(): Unit = {
      presentation.setEnabled(false)
      presentation.setVisible(false)
    }

    val dataContext = actionEvent.getDataContext
    val psiFile = CommonDataKeys.PSI_FILE.getData(dataContext)
    if (HaskellProjectUtil.isHaskellProject(psiFile.getProject)) {
      psiFile match {
        case _: HaskellFile if !onlyForSourceFile => enable()
        case _: HaskellFile if onlyForSourceFile && HaskellProjectUtil.isSourceFile(psiFile) => enable()
        case _ => disable()
      }
    } else {
      disable()
    }
  }

  def showHint(editor: Editor, @HintText text: String, sticky: Boolean = false): Unit = {
    val label = HintUtil.createInformationLabel(text)
    label.setFont(UIUtil.getLabelFont(FontSize.NORMAL))

    val hint = new LightweightHint(label)
    val hintManager = HintManagerImpl.getInstanceImpl

    label.addMouseMotionListener(new MouseMotionAdapter {
      override def mouseMoved(e: MouseEvent): Unit = {
        hintManager.hideAllHints()
      }
    })

    val position = editor.getCaretModel.getLogicalPosition
    val point = HintManagerImpl.getHintPosition(hint, editor, position, HintManager.ABOVE)
    val hintHint = HintManagerImpl.createHintHint(editor, point, hint, HintManager.ABOVE).setExplicitClose(sticky)

    val hideFlags = if (sticky) {
      HintManager.HIDE_BY_ESCAPE
    } else {
      HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_BY_SCROLLING
    }

    hintManager.showEditorHint(hint, editor, point, hideFlags, 0, false, hintHint)
  }

  def showList(messages: Seq[String], editor: Editor): Unit = {
    UIUtil.invokeLaterIfNeeded(() => {
      val listPopupStep: BaseListPopupStep[String] = new BaseListPopupStep[String]("info", messages.asJava) {
        override def isSpeedSearchEnabled: Boolean = true
      }
      val listPopup = JBPopupFactory.getInstance().createListPopup(listPopupStep)
      listPopup.showInBestPositionFor(editor)
    })
  }

  def showStatusBarMessage(project: Project, message: String): Unit = {
    for {
      wm <- Option(WindowManager.getInstance())
      sb <- Option(wm.getStatusBar(project))
    } yield sb.setInfo(message)
  }

  def showHaskellSupportIsNotAvailableWhileInitializing(project: Project): Unit = {
    HaskellEditorUtil.showStatusBarMessage(project, HaskellSupportIsNotAvailableWhileInitializingText)
  }
}
