/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.runconfig

import com.intellij.execution.CantRunException
import com.intellij.execution.configurations.{CommandLineState, GeneralCommandLine}
import com.intellij.execution.process.{KillableColoredProcessHandler, ProcessHandler, ProcessTerminatedListener}
import com.intellij.execution.runners.ExecutionEnvironment
import me.fornever.haskeletor.sdk.HaskellSdkType
import me.fornever.haskeletor.settings.GlobalInfo

import scala.jdk.CollectionConverters._

class HaskellStackStateBase(val configuration: HaskellStackConfigurationBase, val environment: ExecutionEnvironment, val parameters: List[String]) extends CommandLineState(environment) {

  protected def startProcess: ProcessHandler = {
    val project = configuration.getProject

    HaskellSdkType.getStackPath(project) match {
      case Some(stackPath) =>
        val stackArgs = configuration.getStackArgs

        val commandLine = new GeneralCommandLine(stackPath)
          .withParameters(parameters.asJava)
          .withWorkDirectory(configuration.getWorkingDirPath)
          .withEnvironment(GlobalInfo.pathVariables)

        if (stackArgs.nonEmpty)
          commandLine.addParameters(stackArgs.split(" ").toList.asJava)

        val handler = new KillableColoredProcessHandler(commandLine)
        ProcessTerminatedListener.attach(handler)
        handler
      case None => throw new CantRunException("Invalid Haskell Stack SDK")
    }
  }
}
