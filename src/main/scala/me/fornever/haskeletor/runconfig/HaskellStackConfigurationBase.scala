/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.runconfig

import com.intellij.execution.configurations.{ConfigurationFactory, LocatableConfigurationBase}
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element

abstract class HaskellStackConfigurationBase(name: String, project: Project, configurationFactory: ConfigurationFactory)
  extends LocatableConfigurationBase[HaskellStackConfigurationBase](project, configurationFactory, name) {

  private var workingDirPath: String = ""
  private var stackArgs: String = ""

  override def writeExternal(element: Element): Unit = {
    super.writeExternal(element)
    XmlSerializer.serializeInto(this, element)
  }

  override def readExternal(element: Element): Unit = {
    super.readExternal(element)
    XmlSerializer.deserializeInto(this, element)
  }

  def setWorkingDirPath(workingDirPath: String): Unit = {
    this.workingDirPath = workingDirPath
  }

  def getWorkingDirPath: String = {
    if (workingDirPath.isEmpty) {
      project.getBasePath
    } else {
      workingDirPath
    }
  }

  def setStackArgs(stackArgs: String): Unit = {
    this.stackArgs = stackArgs
  }

  def getStackArgs: String = stackArgs
}
