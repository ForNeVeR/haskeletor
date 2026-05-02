/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.module

import com.intellij.openapi.project.Project
import me.fornever.haskeletor.stackyaml.StackYamlComponent

object StackProjectModelSupport {

  private final val projectRootRelativePath = "."

  def getPackagePaths(project: Project): Seq[String] = {
    StackYamlComponent.getPackagePaths(project).getOrElse(Seq(projectRootRelativePath))
  }
}
