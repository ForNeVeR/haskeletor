/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.stack

import com.intellij.build.BuildContentManager
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.ui.content.ContentFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.fornever.haskeletor.core.HaskeletorBundle
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * Shows a focused tab in the Build tool window with a terminal-style console rendering the stderr
 * of a failed Haskell tool installation.
 */
@Service(Service.Level.PROJECT)
class ToolInstallFailureBuildView(private val project: Project) {

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ToolInstallFailureBuildView = project.service()
    }

    /** Show a new, focused tab in the Build tool window with [stderr] rendered in a terminal-style console. */
    suspend fun showFailure(@NlsSafe toolName: String, stderr: String) =
        withContext(Dispatchers.EDT) {
            val console = ConsoleViewImpl(project, viewer = true)
            console.print(stderr, ConsoleViewContentType.ERROR_OUTPUT)

            val panel = JPanel(BorderLayout()).apply { add(console.component, BorderLayout.CENTER) }
            val tabName = HaskeletorBundle.message("build-view.install-tool-failed.tab-title", toolName)
            val content = ContentFactory.getInstance().createContent(panel, tabName, /* isLockable = */ true).apply {
                isCloseable = true
                setDisposer(console)
            }

            val cm = BuildContentManager.getInstance(project)
            cm.addContent(content)
            cm.setSelectedContent(
                content,
                /* requestFocus = */ true,
                /* forcedFocus = */ true,
                /* activate = */ true
            ) {}
        }
}
