/*
 * SPDX-FileCopyrightText: 2000-2015 JetBrains s.r.o.
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.vcs

import com.intellij.openapi.application.EDT
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.CommitCheck
import com.intellij.openapi.vcs.checkin.CommitInfo
import com.intellij.openapi.vcs.checkin.CommitProblem
import com.intellij.openapi.vcs.ui.RefreshableOnComponent
import com.intellij.ui.NonFocusableCheckBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.fornever.haskeletor.core.language.HaskellImportOptimizerService
import me.fornever.haskeletor.core.language.PsiFileUtil
import me.fornever.haskeletor.settings.HaskellSettingsState
import java.awt.GridLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel

class HaskellOptimizeImportsBeforeCheckinHandler(
    private val project: Project,
    private val checkinProjectPanel: CheckinProjectPanel
) : CheckinHandler(), CommitCheck {

    override fun getBeforeCheckinConfigurationPanel(): RefreshableOnComponent = ConfigurationPanel()

    private inner class ConfigurationPanel : RefreshableOnComponent {

        val optimizeBox = NonFocusableCheckBox("Haskell optimize imports")
        init {
            disableWhenDumb(project, optimizeBox, "Impossible until indices are up-to-date")
        }

        override fun getComponent(): JComponent {
            val panel = JPanel(GridLayout(1, 0))
            panel.add(optimizeBox)
            return panel
        }

        override fun saveState() {
            HaskellSettingsState.setOptimizeImportsBeforeCommit(optimizeBox.isSelected)
        }

        override fun restoreState() {
            optimizeBox.isSelected = HaskellSettingsState.isOptmizeImportsBeforeCommit()
        }
    }

    override fun getExecutionOrder(): CommitCheck.ExecutionOrder = CommitCheck.ExecutionOrder.MODIFICATION
    override fun isEnabled(): Boolean = HaskellSettingsState.isReformatCodeBeforeCommit() && !DumbService.isDumb(project)

    override suspend fun runCheck(commitInfo: CommitInfo): CommitProblem? {
        val virtualFiles = checkinProjectPanel.virtualFiles

        withContext(Dispatchers.EDT) {
            virtualFiles.all { vf ->
                PsiFileUtil.convertToHaskellFileDispatchThread(project, vf)
                    .exists { file -> HaskellImportOptimizerService.getInstance().removeRedundantImports(file) }
            }
            FileDocumentManager.getInstance().saveAllDocuments()
        }

        return null
    }

    private fun disableWhenDumb(project: Project, checkBox: JCheckBox, tooltip: String) {
        val dumb = DumbService.isDumb(project)
        checkBox.isEnabled = !dumb
        checkBox.toolTipText = if (dumb) tooltip else ""
    }
}
