/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.external.repl

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import me.fornever.haskeletor.external.repl.StackRepl.StackReplOutput
import me.fornever.haskeletor.external.repl.StackReplsManager.ProjectReplTargets
import me.fornever.haskeletor.psi.HaskellPsiUtil
import me.fornever.haskeletor.settings.HaskellSettingsState
import me.fornever.haskeletor.util.ScalaFutureUtil

import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.{Future, blocking}
import scala.jdk.CollectionConverters._

case class ProjectStackRepl(project: Project, projectReplTargets: ProjectReplTargets, replTimeout: Int) extends StackRepl(project, Some(projectReplTargets), Seq(), replTimeout: Int, HaskellSettingsState.getDefaultGhcOptions) {

  import me.fornever.haskeletor.external.repl.ProjectStackRepl._

  val target: String = projectReplTargets.targetsName

  val stanzaType: StackRepl.StanzaType = projectReplTargets.stanzaType

  def clearLoadedModules(): Unit = {
    loadedFile = None
    loadedDependentModules.clear()
    everLoadedDependentModules.clear()
  }

  def clearLoadedModule(): Unit = {
    loadedFile = None
  }

  private case class ModuleInfo(psiFile: PsiFile, loadFailed: Boolean)

  @volatile
  private[this] var loadedFile: Option[ModuleInfo] = None

  private case class DependentModuleInfo()

  private type ModuleName = String
  private[this] val loadedDependentModules = new ConcurrentHashMap[ModuleName, DependentModuleInfo]().asScala
  private[this] val everLoadedDependentModules = new ConcurrentHashMap[ModuleName, DependentModuleInfo]().asScala

  import scala.concurrent.ExecutionContext.Implicits.global

  def findTypeInfo(moduleName: Option[String], psiFile: PsiFile, startLineNr: Int, startColumnNr: Int, endLineNr: Int, endColumnNr: Int, expression: String): Option[StackReplOutput] = {
    val filePath = getFilePath(psiFile)

    def execute = {
      blocking {
        executeModuleLoadedCommand(moduleName, psiFile, s":type-at $filePath $startLineNr $startColumnNr $endLineNr $endColumnNr $expression")
      }
    }

    ScalaFutureUtil.waitForValue(project, Future(execute), ":type-at in ProjectStackRepl").flatten
  }

  def findLocationInfo(moduleName: Option[String], psiFile: PsiFile, startLineNr: Int, startColumnNr: Int, endLineNr: Int, endColumnNr: Int, expression: String): Option[StackReplOutput] = {
    val filePath = getFilePath(psiFile)

    def execute = {
      blocking {
        executeModuleLoadedCommand(moduleName, psiFile, s":loc-at $filePath $startLineNr $startColumnNr $endLineNr $endColumnNr $expression")
      }
    }

    ScalaFutureUtil.waitForValue(project, Future(execute), ":loc-at in ProjectStackRepl").flatten
  }

  def findInfo(psiFile: PsiFile, name: String): Option[StackReplOutput] = {
    val moduleName = HaskellPsiUtil.findModuleName(psiFile)

    def execute = {
      blocking {
        executeWithLoad(psiFile, moduleName, s":info $name")
      }
    }

    ScalaFutureUtil.waitForValue(psiFile.getProject, Future(execute), ":info in ProjectStackRepl").flatten
  }

  def isModuleLoaded(moduleName: String): Boolean = {
    everLoadedDependentModules.contains(moduleName)
  }

  def isBrowseModuleLoaded(moduleName: String): Boolean = {
    loadedDependentModules.contains(moduleName)
  }

  def isFileLoaded(psiFile: PsiFile): IsFileLoaded = {
    loadedFile match {
      case Some(info) if psiFile == info.psiFile && !info.loadFailed => Loaded
      case Some(info) if psiFile == info.psiFile && info.loadFailed => Failed
      case Some(_) => OtherFileIsLoaded
      case None => NoFileIsLoaded
    }
  }

  private def setLoadedModules(output: StackReplOutput): Unit = {
    loadedDependentModules.clear()
    output.stdoutLines.lastOption.foreach(line =>
      if (!line.contains("modules loaded: none.") && line.contains("modules loaded: ")) {
        val modulesLine = line.split("loaded:")(1).init // The init to get rid of the dot which is last character
        val loadedModuleNames = modulesLine.split(",").map(_.trim)
        loadedModuleNames.foreach(mn => loadedDependentModules.put(mn, DependentModuleInfo()))
        loadedModuleNames.foreach(mn => everLoadedDependentModules.put(mn, DependentModuleInfo()))
      })
  }

  def load(psiFile: PsiFile, fileModified: Boolean, moduleName: Option[String], forceNoReload: Boolean = false): Option[(StackReplOutput, Boolean)] = synchronized {
    val filePath = getFilePath(psiFile)

    val reload = if (!fileModified || forceNoReload) {
      false
    } else if (fileModified) {
      val loaded = isFileLoaded(psiFile)
      loaded == Loaded || loaded == Failed
    } else if (!fileModified && isFileLoaded(psiFile) != Loaded) {
      false
    } else moduleName.exists(mn => loadedDependentModules.contains(mn))

    val output = if (reload) {
      execute(s":reload")
    } else {
      execute(s":load *$filePath")
    }

    output match {
      case Some(o) =>
        val loadFailed = isLoadFailed(o)
        setLoadedModules(o)

        loadedFile = Some(ModuleInfo(psiFile, loadFailed))
        Some(o, loadFailed)
      case _ =>
        loadedDependentModules.clear()
        loadedFile = None
        None
    }
  }

  def getModuleIdentifiers(project: Project, moduleName: String, psiFile: Option[PsiFile]): Option[StackReplOutput] = {
    ScalaFutureUtil.waitForValue(project,
      Future {
        blocking {
          synchronized {
            if (psiFile.isEmpty || isBrowseModuleLoaded(moduleName)
              || psiFile.exists(pf => load(pf, fileModified = false, Some(moduleName)).exists(_._2 == false))
            ) {
              execute(s":browse! $moduleName")
            } else {
              HaskellNotificationGroup.logInfoEvent(project, s"Couldn't get module identifiers for module $moduleName because file ${psiFile.map(_.getName).getOrElse("-")} isn't loaded")
              None
            }
          }
        }
      }, "getModuleIdentifiers in ProjectStackRepl").flatten
  }

  override def restart(forceExit: Boolean): Unit = synchronized {
    if (available && !starting) {
      exit(forceExit)
      start()
    }
  }

  private def executeModuleLoadedCommand(moduleName: Option[String], psiFile: PsiFile, command: String): Option[StackReplOutput] = synchronized {
    if (moduleName.exists(isModuleLoaded)) {
      execute(command)
    } else {
      executeWithLoad(psiFile, moduleName, command)
    }
  }

  private def executeWithLoad(psiFile: PsiFile, moduleName: Option[String], command: String): Option[StackReplOutput] = synchronized {
    loadedFile match {
      case Some(info) if info.psiFile == psiFile && !info.loadFailed => execute(command)
      case _ =>
        load(psiFile, fileModified = false, moduleName)
        loadedFile match {
          case None => None
          case Some(info) if info.psiFile == psiFile => execute(command)
          case _ => None
        }
    }
  }

  private def isLoadFailed(output: StackReplOutput): Boolean = {
    output.stdoutLines.lastOption.exists(_.contains("Failed, "))
  }

  private def getFilePath(psiFile: PsiFile): String = {
    HaskellFileUtil.getAbsolutePath(psiFile) match {
      case Some(filePath) =>
        if (filePath.contains(" ")) {
          s""""$filePath""""
        } else {
          filePath
        }
      case None => throw new IllegalStateException(s"Can't load file `${psiFile.getName}` in REPL because it only exists in memory")
    }
  }
}

object ProjectStackRepl {

  sealed trait IsFileLoaded

  case object Loaded extends IsFileLoaded

  case object Failed extends IsFileLoaded

  case object NoFileIsLoaded extends IsFileLoaded

  case object OtherFileIsLoaded extends IsFileLoaded

}
