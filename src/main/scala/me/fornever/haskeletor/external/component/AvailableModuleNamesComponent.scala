/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.external.component

import com.github.blemale.scaffeine.{AsyncLoadingCache, Scaffeine}
import com.intellij.openapi.project.{IndexNotReadyException, Project}
import com.intellij.psi.search.{FileTypeIndex, GlobalSearchScope}
import me.fornever.haskeletor.HaskellFileType
import me.fornever.haskeletor.core.notifications.HaskellNotificationGroup
import me.fornever.haskeletor.external.component.HaskellComponentsManager.ComponentTarget
import me.fornever.haskeletor.psi.HaskellPsiUtil
import me.fornever.haskeletor.util.{ApplicationUtil, HaskellFileUtil, ScalaFutureUtil}

import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

object AvailableModuleNamesComponent {

  private case class Key(project: Project, target: String)

  private final val Cache: AsyncLoadingCache[Key, Iterable[String]] = Scaffeine().expireAfterWrite(1.second).buildAsync((k: Key) => findAvailableProjectModuleNamesWithIndex(k))

  def findAvailableModuleNamesWithIndex(project: Project, stackComponentInfo: ComponentTarget): Iterable[String] = {
    // A module can be a project module AND library module
    findAvailableLibraryModuleNames(project, stackComponentInfo) ++ findAvailableProjectModuleNames(project, stackComponentInfo)
  }

  def findAvailableProjectModuleNames(project: Project, stackComponentInfo: ComponentTarget): Iterable[String] = {
    val key = Key(project, stackComponentInfo.target)
    ScalaFutureUtil.waitForValue(project, Cache.get(key), s"getting project module names for target ${key.target}", 1.second) match {
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
    findModuleNames(project)
  }

  private def findAvailableLibraryModuleNames(project: Project, stackComponentInfo: ComponentTarget): Iterable[String] = {
    HaskellComponentsManager.findStackComponentGlobalInfo(project, stackComponentInfo).map(_.packageInfos.flatMap(_.exposedModuleNames)).getOrElse(Iterable())
  }

  def findModuleNames(project: Project): Iterable[String] = {
    for {
      vf <- findHaskellFiles(project)
      hf <- HaskellFileUtil.convertToHaskellFileInReadAction(project, vf).toSeq
      mn <- HaskellPsiUtil.findModuleName(hf)
    } yield mn
  }

  private def findHaskellFiles(project: Project) = {
    ApplicationUtil.runReadActionWithFileAccess(project, {
      try {
        val searchScope = GlobalSearchScope.projectScope(project)
        FileTypeIndex.getFiles(HaskellFileType.INSTANCE, searchScope).asScala
      } catch {
        case _: IndexNotReadyException =>
          HaskellNotificationGroup.logInfoEvent(project, s"Index not ready while findHaskellFiles for project ${project.getName} ")
          Iterable()
      }
    }, s"find Haskell files for module project ${project.getName}").toOption.toIterable.flatten
  }


}


