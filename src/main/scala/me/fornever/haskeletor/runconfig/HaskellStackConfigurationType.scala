/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.runconfig

import com.intellij.execution.configurations.{ConfigurationFactory, ConfigurationType}
import me.fornever.haskeletor.icons.HaskellIcons
import me.fornever.haskeletor.runconfig.console.HaskellConsoleConfigurationFactory
import me.fornever.haskeletor.runconfig.run.HaskellRunConfigurationFactory
import me.fornever.haskeletor.runconfig.test.HaskellTestConfigurationFactory

import javax.swing.Icon

class HaskellStackConfigurationType extends ConfigurationType:
  def getDisplayName: String = "Haskell Stack"

  def getConfigurationTypeDescription: String = "Haskell Stack configuration"

  def getIcon: Icon = HaskellIcons.HaskellLogo

  def getId = "HaskellStackConfigurationType"

  def getConfigurationFactories: Array[ConfigurationFactory] = Array[ConfigurationFactory](
    new HaskellConsoleConfigurationFactory(this),
    new HaskellRunConfigurationFactory(this),
    new HaskellTestConfigurationFactory(this)
  )
