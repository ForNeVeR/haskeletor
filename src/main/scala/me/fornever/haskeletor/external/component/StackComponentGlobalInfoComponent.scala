/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.external.component

import com.github.blemale.scaffeine.{AsyncLoadingCache, Scaffeine}
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import me.fornever.haskeletor.external.component.HaskellComponentsManager.ComponentTarget
import me.fornever.haskeletor.util.{HaskellProjectUtil, ScalaFutureUtil}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future, blocking}

@Service(Array(Service.Level.PROJECT))
final class StackComponentGlobalInfoComponent(project: Project) {

  import scala.concurrent.ExecutionContext.Implicits.global

  private case class Key(stackComponentInfo: ComponentTarget)

  private type Result = Option[StackComponentGlobalInfo]

  private val Cache: AsyncLoadingCache[Key, Result] = Scaffeine().buildAsync((k: Key) => createStackInfo(k))

  def findStackComponentGlobalInfo(stackComponentInfo: ComponentTarget): Option[StackComponentGlobalInfo] = {
    val key = Key(stackComponentInfo)
    ScalaFutureUtil.waitForValue(project, Cache.get(key), "Getting global info").flatten match {
      case result@Some(_) => result
      case _ =>
        Cache.synchronous().invalidate(key)
        None
    }
  }

  private def createStackInfo(key: Key): Result = {
    val stackComponentInfo = key.stackComponentInfo
    findAvailableLibraryModuleNames(stackComponentInfo)
  }

  private def findAvailableLibraryModuleNames(componentInfo: ComponentTarget): Result = {
    val projectPackageNames = HaskellProjectUtil.findProjectPackageNames(project)
    val buildDependsLibraryPackages = componentInfo.buildDepends.filterNot(projectPackageNames.contains) ++ Seq("ghc-prim")

    val libraryModuleNamesFutures = buildDependsLibraryPackages.grouped(5).map { packageNames =>
      Future {
        blocking {
          packageNames.flatMap { packageName =>
            if (project.isDisposed) {
              None
            } else {
              LibraryPackageInfoComponent.findLibraryPackageInfo(project, packageName)
            }
          }
        }
      }
    }

    val libraryModuleNames = Await.result(Future.sequence(libraryModuleNamesFutures), 60.second).flatten.toSeq

    Some(StackComponentGlobalInfo(componentInfo, libraryModuleNames))
  }

  def invalidate(): Unit = {
    Cache.synchronous().invalidateAll()
  }
}

object StackComponentGlobalInfoComponent {
  def getInstance(project: Project): StackComponentGlobalInfoComponent = project.getService(classOf[StackComponentGlobalInfoComponent])
}

case class StackComponentGlobalInfo(stackComponentInfo: ComponentTarget, packageInfos: Seq[LibraryPackageInfo])
