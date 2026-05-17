/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.projectmodel

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

interface HaskellProjectInitializer {

    companion object {
        fun getInstance(project: Project): HaskellProjectInitializer = project.service()
    }

    fun projectOpened()
}
