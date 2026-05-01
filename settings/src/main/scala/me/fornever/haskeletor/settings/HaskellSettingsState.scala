/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.settings

object HaskellSettingsState {
  private def state = HaskellSettingsPersistentStateComponent.getInstance().getState

  def getReplTimeout: Integer = {
    state.replTimeout
  }

  def getHlintOptions: String = {
    state.hlintOptions
  }

  def useSystemGhc: Boolean = {
    state.useSystemGhc
  }

  def getNewProjectTemplateName: String = {
    state.newProjectTemplateName
  }

  def getCachePath: String = {
    state.cachePath
  }

  def isReformatCodeBeforeCommit: Boolean = {
    state.reformatCodeBeforeCommit
  }

  def setReformatCodeBeforeCommit(reformat: Boolean): Unit = {
    state.reformatCodeBeforeCommit = reformat
  }

  def isOptmizeImportsBeforeCommit: Boolean = {
    state.optimizeImportsBeforeCommit
  }

  def setOptimizeImportsBeforeCommit(optimize: Boolean): Unit = {
    state.optimizeImportsBeforeCommit = optimize
  }

  def customTools: Boolean = {
    state.customTools
  }

  def hlintPath: Option[String] = {
    Option.when(customTools && state.hlintPath.nonEmpty)(state.hlintPath)
  }

  def hooglePath: Option[String] = {
    Option.when(customTools && state.hooglePath.nonEmpty)(state.hooglePath)
  }

  def ormoluPath: Option[String] = {
    Option.when(customTools && state.ormoluPath.nonEmpty)(state.ormoluPath)
  }

  def stylishHaskellPath: Option[String] = {
    Option.when(customTools && state.stylishHaskellPath.nonEmpty)(state.stylishHaskellPath)
  }

  def useCustomTools: Boolean = {
    state.customTools
  }

  def getExtraStackArguments: java.util.List[String] = {
    import scala.jdk.CollectionConverters._
    Option.when(state.extraStackArguments.trim.nonEmpty)(state.extraStackArguments)
      .map(_.split("""\s+""").toSeq)
      .getOrElse(Seq())
      .asJava
  }

  def getDefaultGhcOptions: Seq[String] = {
    state.defaultGhcOptions.split(" ").map(_.trim)
  }
}
