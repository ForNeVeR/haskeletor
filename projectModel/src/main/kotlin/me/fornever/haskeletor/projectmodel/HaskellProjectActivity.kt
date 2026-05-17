/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.projectmodel

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class HaskellProjectActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        HaskellProjectInitializer.getInstance(project).projectOpened()
    }
}
