/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.external.component

import com.github.blemale.scaffeine.{AsyncLoadingCache, Scaffeine}
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.{IndexNotReadyException, Project}
import com.intellij.psi.search.{FileTypeIndex, GlobalSearchScope}
import me.fornever.haskeletor.HaskellFileType
import me.fornever.haskeletor.external.component.HaskellComponentsManager.ComponentTarget
import me.fornever.haskeletor.external.repl.StackRepl.{BenchmarkType, TestSuiteType}
import me.fornever.haskeletor.psi.HaskellPsiUtil
import me.fornever.haskeletor.util.{ApplicationUtil, HaskellFileUtil, ScalaFutureUtil}

import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

object AvailableModuleNamesComponent {

  private final val TestStanzaTypes = Seq(TestSuiteType, BenchmarkType)

  private case class Key(project: Project, target: String)

  private final val Cache: AsyncLoadingCache[Key, Iterable[String]] = Scaffeine().expireAfterWrite(1.second).buildAsync((k: Key) => findAvailableProjectModuleNamesWithIndex(k))

  def findAvailableModuleNamesWithIndex(stackComponentInfo: ComponentTarget): Iterable[String] = {
    // A module can be a project module AND library module
    findAvailableLibraryModuleNames(stackComponentInfo) ++ findAvailableProjectModuleNames(stackComponentInfo)
  }

  def findAvailableModuleLibraryModuleNamesWithIndex(module: Module): Iterable[String] = {
    findModuleNamesInModule(module.getProject, module, Seq.empty, includeTests = false)
  }

  def findAvailableProjectModuleNames(stackComponentInfo: ComponentTarget): Iterable[String] = {
    val key = Key(stackComponentInfo.module.getProject, stackComponentInfo.target)
    ScalaFutureUtil.waitForValue(stackComponentInfo.module.getProject, Cache.get(key), s"getting project module names for target ${key.target}", 1.second) match {
      case Some(files) => files
      case _ =>
        Cache.synchronous().invalidate(key)
        Iterable()
    }
  }

  def isProjectModule(project: Project, moduleName: String): Boolean = {
    val moduleNames = HaskellComponentsManager.findStackComponentInfos(project).flatMap(info => findAvailableProjectModuleNamesWithIndex(Key(project, info.target)))
    moduleNames.contains(moduleName)
  }

  private def findAvailableProjectModuleNamesWithIndex(key: Key): Iterable[String] = {
    val project = key.project
    val projectModulePackageNames = HaskellComponentsManager.findProjectModulePackageNames(project)
    HaskellComponentsManager.findStackComponentInfos(project).find(info => info.module.getProject == project && info.target == key.target).map { stackComponentInfo =>
      val libraryProjectModules = projectModulePackageNames.filter { case (_, n) => stackComponentInfo.buildDepends.contains(n) }.map(_._1)
      findModuleNamesInModule(stackComponentInfo.module.getProject, stackComponentInfo.module, libraryProjectModules, TestStanzaTypes.contains(stackComponentInfo.stanzaType))
    }.getOrElse(Iterable())
  }

  private def findAvailableLibraryModuleNames(stackComponentInfo: ComponentTarget): Iterable[String] = {
    HaskellComponentsManager.findStackComponentGlobalInfo(stackComponentInfo).map(_.packageInfos.flatMap(_.exposedModuleNames)).getOrElse(Iterable())
  }

  private def findModuleNamesInModule(project: Project, currentModule: Module, modules: Seq[Module], includeTests: Boolean): Iterable[String] = {
    for {
      vf <- findHaskellFiles(project, currentModule, modules, includeTests)
      hf <- HaskellFileUtil.convertToHaskellFileInReadAction(project, vf).toSeq
      mn <- HaskellPsiUtil.findModuleName(hf)
    } yield mn
  }

  private def findHaskellFiles(project: Project, currentModule: Module, projectModules: Seq[Module], includeTests: Boolean) = {
    ApplicationUtil.runReadActionWithFileAccess(project, {
      try {
        val projectModulesScope = projectModules.foldLeft(GlobalSearchScope.EMPTY_SCOPE)({ case (x, y) => x.uniteWith(y.getModuleScope(false)) })
        val searchScope = currentModule.getModuleScope(includeTests).uniteWith(projectModulesScope)
        FileTypeIndex.getFiles(HaskellFileType.INSTANCE, searchScope).asScala
      } catch {
        case _: IndexNotReadyException =>
          HaskellNotificationGroup.logInfoEvent(project, s"Index not ready while findHaskellFiles for module ${currentModule.getName} ")
          Iterable()
      }
    }, s"find Haskell files for module ${currentModule.getName}").toOption.toIterable.flatten
  }


}


