/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.runconfig.console

import com.intellij.execution.Executor
import com.intellij.execution.configurations._
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import me.fornever.haskeletor.runconfig.HaskellStackConfigurationBase

import java.lang

class HaskellConsoleConfiguration(name: String, project: Project, configurationFactory: ConfigurationFactory)
  extends HaskellStackConfigurationBase(name, project, configurationFactory) {

  private var stackTarget: String = ""
  val replCommand = "ghci"

  override def getConfigurationEditor = new HaskellConsoleConfigurationForm(getProject)

  override def getState(executor: Executor, environment: ExecutionEnvironment) = new HaskellConsoleState(this, environment)

  def getStackTargetNames: lang.Iterable[String] = {
    HaskellComponentsManager.findCabalInfos(project).flatMap(_.cabalStanzas.map(_.targetName)).asJava
  }

  def setStackTarget(target: String): Unit = {
    stackTarget = target
  }

  def getStackTarget: String = {
    stackTarget
  }
}
