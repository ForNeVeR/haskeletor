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
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.CheckinMetaHandler
import com.intellij.openapi.vcs.ui.RefreshableOnComponent
import com.intellij.ui.NonFocusableCheckBox
import me.fornever.haskeletor.core.language.HaskellReformatService
import me.fornever.haskeletor.core.language.PsiFileUtil
import me.fornever.haskeletor.settings.HaskellSettingsState
import java.awt.GridLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel

class HaskellReformatBeforeCheckinHandler(
    private val project: Project,
    private val checkinProjectPanel: CheckinProjectPanel
) : CheckinHandler(), CheckinMetaHandler {

    override fun getBeforeCheckinConfigurationPanel(): RefreshableOnComponent {
        val reformatBox = NonFocusableCheckBox("Haskell reformat code")
        disableWhenDumb(project, reformatBox, "Impossible until indices are up-to-date")
        return object : RefreshableOnComponent {
            override fun getComponent(): JComponent {
                val panel = JPanel(GridLayout(1, 0))
                panel.add(reformatBox)
                return panel
            }

            override fun refresh() = Unit

            override fun saveState() {
                HaskellSettingsState.setReformatCodeBeforeCommit(reformatBox.isSelected)
            }

            override fun restoreState() {
                reformatBox.isSelected = HaskellSettingsState.isReformatCodeBeforeCommit()
            }
        }
    }

    override fun runCheckinHandlers(finishAction: Runnable) {
        val virtualFiles = checkinProjectPanel.virtualFiles

        val performCheckoutAction = Runnable {
            FileDocumentManager.getInstance().saveAllDocuments()
            finishAction.run()
        }

        if (HaskellSettingsState.isReformatCodeBeforeCommit() && !DumbService.isDumb(project)) {
            val reformatResult = virtualFiles
                .asSequence()
                .filter { it.extension == "hs" }
                .all { vf ->
                    PsiFileUtil.convertToHaskellFileDispatchThread(project, vf)
                        .exists { file -> HaskellReformatService.getInstance().reformat(file) }
                }
            if (reformatResult) {
                performCheckoutAction.run()
            }
        } else {
            performCheckoutAction.run()
        }
    }

    private fun disableWhenDumb(project: Project, checkBox: JCheckBox, tooltip: String) {
        val dumb = DumbService.isDumb(project)
        checkBox.isEnabled = !dumb
        checkBox.toolTipText = if (dumb) tooltip else ""
    }
}
