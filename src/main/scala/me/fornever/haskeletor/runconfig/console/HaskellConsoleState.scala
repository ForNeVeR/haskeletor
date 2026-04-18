/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.runconfig.console

import com.intellij.execution.CantRunException
import com.intellij.execution.configurations.{CommandLineState, GeneralCommandLine}
import com.intellij.execution.filters.TextConsoleBuilderImpl
import com.intellij.execution.process.{ProcessHandler, ProcessTerminatedListener}
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.ConsoleView
import me.fornever.haskeletor.GlobalInfo
import me.fornever.haskeletor.sdk.HaskellSdkType

import java.io.File

class HaskellConsoleState(val configuration: HaskellConsoleConfiguration, val environment: ExecutionEnvironment) extends CommandLineState(environment) {

  val consoleBuilder: TextConsoleBuilderImpl = new TextConsoleBuilderImpl(configuration.getProject) {
    override def getConsole: ConsoleView = {
      new HaskellConsoleView(configuration.getProject, configuration)
    }
  }
  setConsoleBuilder(consoleBuilder)

  protected def startProcess: ProcessHandler = {
    val project = configuration.getProject

    HaskellSdkType.getStackPath(project) match {
      case Some(stackPath) =>
        val stackTarget = configuration.getStackTarget
        val ghcVersion = HaskellComponentsManager.getGhcVersion(project)
        val ghc821Compatible = ghcVersion.exists(_ >= GhcVersion(8, 2, 1))
        val ghciScriptName = if (ghc821Compatible) "8.2.1.ghci" else "default.ghci"
        val ghciScript = new File(GlobalInfo.getHaskeletorDirectory, ghciScriptName)

        if (!ghciScript.exists()) {
          HaskellFileUtil.copyStreamToFile(getClass.getResourceAsStream(s"/ghci/$ghciScriptName"), ghciScript)
          ghciScript.setWritable(true, true)
          HaskellFileUtil.removeGroupWritePermission(ghciScript)
        }

        val commandLine = new GeneralCommandLine(stackPath)
          .withParameters(configuration.replCommand, "--ghci-options", s"-ghci-script ${ghciScript.getAbsolutePath}")
          .withWorkDirectory(project.getBasePath)
          .withEnvironment(GlobalInfo.pathVariables)

        if (stackTarget.nonEmpty) {
          commandLine.addParameter(stackTarget)
        }

        // Enable color output for GHC versions that support it.
        if (ghc821Compatible) {
          commandLine.addParameters("--ghc-options", "-fdiagnostics-color=always")
        }

        commandLine.setRedirectErrorStream(true)

        val handler = new HaskellConsoleProcessHandler(commandLine.createProcess(), commandLine.getCommandLineString(), consoleBuilder.getConsole.asInstanceOf[HaskellConsoleView])
        ProcessTerminatedListener.attach(handler)
        handler
      case None => throw new CantRunException("Invalid Haskell Stack SDK")
    }
  }
}
