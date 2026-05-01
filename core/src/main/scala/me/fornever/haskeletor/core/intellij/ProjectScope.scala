/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.core.intellij

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope

object ProjectScope {
  def get(project: Project): CoroutineScope = {
    project.getService(classOf[ProjectScopeHolder]).coroutineScope
  }
}

@Service(Array(Service.Level.PROJECT))
private final class ProjectScopeHolder(val coroutineScope: CoroutineScope)
