/*
 * SPDX-FileCopyrightText: 2000-2015 JetBrains s.r.o.
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.vcs

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.{DumbService, Project}
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.checkin._
import com.intellij.openapi.vcs.ui.RefreshableOnComponent
import com.intellij.ui.NonFocusableCheckBox
import me.fornever.haskeletor.core.language.{HaskellReformatService, PsiFileUtil}
import me.fornever.haskeletor.settings.HaskellSettingsState

import java.awt.GridLayout
import javax.swing.{JCheckBox, JComponent, JPanel}

class HaskellReformatBeforeCheckinHandler(project: Project, checkinProjectPanel: CheckinProjectPanel) extends CheckinHandler with CheckinMetaHandler {

  override def getBeforeCheckinConfigurationPanel: RefreshableOnComponent = {

    val reformatBox = new NonFocusableCheckBox("Haskell reformat code")
    disableWhenDumb(project, reformatBox, "Impossible until indices are up-to-date")
    new RefreshableOnComponent() {
      override def getComponent: JComponent = {
        val panel = new JPanel(new GridLayout(1, 0))
        panel.add(reformatBox)
        panel
      }

      override def refresh(): Unit = {
      }

      override def saveState(): Unit = {
        HaskellSettingsState.setReformatCodeBeforeCommit(reformatBox.isSelected)
      }

      override def restoreState(): Unit = {
        reformatBox.setSelected(HaskellSettingsState.isReformatCodeBeforeCommit)
      }
    }
  }

  override def runCheckinHandlers(finishAction: Runnable): Unit = {
    import scala.jdk.CollectionConverters._
    val virtualFiles = checkinProjectPanel.getVirtualFiles

    val performCheckoutAction: Runnable = () => {
      FileDocumentManager.getInstance.saveAllDocuments()
      finishAction.run()
    }

    if (HaskellSettingsState.isReformatCodeBeforeCommit && !DumbService.isDumb(project)) {
      val reformatResult = virtualFiles.asScala
        .filter(vf => vf.getExtension == "hs")
        .forall(vf =>
          PsiFileUtil.convertToHaskellFileDispatchThread(project, vf)
            .exists(file => HaskellReformatService.getInstance().reformat(file))
        )
      if (reformatResult) {
        performCheckoutAction.run()
      }
    } else {
      performCheckoutAction.run()
    }
  }


  private def disableWhenDumb(project: Project, checkBox: JCheckBox, tooltip: String): Unit = {
    val dumb = DumbService.isDumb(project)
    checkBox.setEnabled(!dumb)
    checkBox.setToolTipText(if (dumb) tooltip else "")
  }
}
