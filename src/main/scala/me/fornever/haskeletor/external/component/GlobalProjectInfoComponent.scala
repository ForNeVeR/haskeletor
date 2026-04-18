/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.external.component

import com.github.blemale.scaffeine.{LoadingCache, Scaffeine}
import com.intellij.openapi.project.Project
import me.fornever.haskeletor.external.execution.{CommandLine, StackCommandLine}
import me.fornever.haskeletor.util.ScalaUtil

import java.io.File
import scala.jdk.CollectionConverters._

private[component] object GlobalProjectInfoComponent {

  private case class Key(project: Project)

  private final val Cache: LoadingCache[Key, Option[GlobalProjectInfo]] = Scaffeine().build((k: Key) => createGlobalProjectInfo(k))

  def findGlobalProjectInfo(project: Project): Option[GlobalProjectInfo] = {
    val key = Key(project)
    Cache.get(key) match {
      case result@Some(_) => result
      case _ =>
        Cache.invalidate(key)
        None
    }
  }

  def getSupportedLanguageExtensions(project: Project, ghcPath: String): Seq[String] = {
    CommandLine.run(
      project,
      ghcPath,
      Seq("--supported-languages"),
      notifyBalloonError = true
    ).getStdoutLines.asScala.toSeq
  }

  def getAvailableStackagesPackages(project: Project): Iterable[String] = {
    CabalConfigComponent.getAvailablePackageNames(project)
  }

  def invalidate(project: Project): Unit = {
    val keys = Cache.asMap().keys.filter(_.project == project)
    keys.foreach(Cache.invalidate)
  }

  private def createGlobalProjectInfo(key: Key): Option[GlobalProjectInfo] = {
    val project = key.project
    for {
      pathLines <- findPathLines(project)
      pathInfoMap = ScalaUtil.linesToMap(pathLines)
      binPaths <- findBinPaths(pathInfoMap)
      packageDbPaths <- findPackageDbPaths(pathInfoMap)
      ghcPath = new File(binPaths.compilerBinPath, "ghc").getPath
      ghcPkgPath = new File(binPaths.compilerBinPath, "ghc-pkg").getPath
      extensions = getSupportedLanguageExtensions(project, ghcPath)
      stackagePackageNames = getAvailableStackagesPackages(project)
      ghcVersion = findGhcVersion(project, ghcPath)
      localDocRoot <- pathInfoMap.get("local-doc-root")
      snapshotDocRoot <- pathInfoMap.get("snapshot-doc-root")
    } yield GlobalProjectInfo(ghcVersion, ghcPath, ghcPkgPath, localDocRoot, snapshotDocRoot, packageDbPaths, binPaths, extensions, stackagePackageNames)
  }

  private def findPathLines(project: Project): Option[Seq[String]] = {
    StackCommandLine.run(project, Seq("path"), enableExtraArguments = false).map(_.getStdoutLines.asScala.toSeq)
  }

  private def findGhcVersion(project: Project, ghcPath: String): GhcVersion = {
    val output = CommandLine.run(project, ghcPath, Seq("--numeric-version"))
    GhcVersion.parse(output.getStdout.trim)
  }

  private def findBinPaths(pathInfoMap: Map[String, String]): Option[ProjectBinPaths] = {
    for {
      compilerBinPath <- pathInfoMap.get("compiler-bin")
      localBinPath <- pathInfoMap.get("local-install-root").map(p => new File(p, "bin").getPath)
    } yield ProjectBinPaths(compilerBinPath, localBinPath)
  }

  private def findPackageDbPaths(pathInfoMap: Map[String, String]): Option[PackageDbPaths] = {
    for {
      globalPackageDbPath <- pathInfoMap.get("global-pkg-db")
      snapshotPackageDbPath <- pathInfoMap.get("snapshot-pkg-db")
      localPackageDbPath <- pathInfoMap.get("local-pkg-db")
    } yield PackageDbPaths(globalPackageDbPath, snapshotPackageDbPath, localPackageDbPath)
  }
}


case class GlobalProjectInfo(ghcVersion: GhcVersion, ghcPath: String, ghcPkgPath: String, localDocRoot: String, snapshotDocRoot: String, packageDbPaths: PackageDbPaths, projectBinPaths: ProjectBinPaths, supportedLanguageExtensions: Iterable[String], availableStackagePackageNames: Iterable[String])

case class PackageDbPaths(globalPackageDbPath: String, snapshotPackageDbPath: String, localPackageDbPath: String)

case class ProjectBinPaths(compilerBinPath: String, localBinPath: String)
