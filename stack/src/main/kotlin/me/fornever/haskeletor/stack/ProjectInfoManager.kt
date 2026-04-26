/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.stack

import me.fornever.haskeletor.core.project.GlobalProjectInfo
import java.util.*

interface ProjectInfoManager {
    fun findGlobalProjectInfo(): Optional<GlobalProjectInfo>
}
