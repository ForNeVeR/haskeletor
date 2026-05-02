/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.external.repl

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.{VfsUtil, VirtualFile}
import com.intellij.psi.PsiFile
import me.fornever.haskeletor.cabal._
import me.fornever.haskeletor.core.notifications.HaskellNotificationGroup
import me.fornever.haskeletor.external.component.HaskellComponentsManager.ComponentTarget
import me.fornever.haskeletor.external.component.{HaskellComponentsManager, StackProjectManager}
import me.fornever.haskeletor.external.repl.StackRepl._
import me.fornever.haskeletor.external.repl.StackReplsManager.ProjectReplTargets
import me.fornever.haskeletor.settings.HaskellSettingsState
import me.fornever.haskeletor.util._

import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters._

private[external] object StackReplsManager {

  case class ProjectReplTargets(stanzaType: StanzaType, targets: Seq[ComponentTarget]) {
    def targetsName: String = targets.map(_.target).mkString(" ")
  }

  def getReplsManager(project: Project): Option[StackReplsManager] = {
    StackProjectManager.getStackProjectManager(project).flatMap(_.getStackReplsManager)
  }

  def getRunningProjectRepls(project: Project): Iterable[ProjectStackRepl] = {
    getReplsManager(project).map(_.getRunningProjectRepls).getOrElse(Iterable())
  }

  def getProjectRepl(psiFile: PsiFile): Option[ProjectStackRepl] = {
    getReplsManager(psiFile.getProject).flatMap(_.findProjectRepl(psiFile))
  }

  def getProjectRepl(project: Project, projectReplTargets: ProjectReplTargets): Option[ProjectStackRepl] = {
    getReplsManager(project).map(_.getProjectRepl(projectReplTargets))
  }

  def getGlobalRepl(project: Project): Option[GlobalStackRepl] = {
    val repl = getReplsManager(project).map(_.getGlobalRepl)
    repl.foreach(r => if (!r.available && !r.starting) r.start())
    repl
  }

  def getGlobalRepl2(project: Project): Option[GlobalStackRepl] = {
    val repl = getReplsManager(project).map(_.getGlobalRepl2)
    repl.foreach(r => if (!r.available && !r.starting) r.start())
    repl
  }

  private def createPackageInfos(project: Project): Iterable[PackageInfo] = {
    val projectRoots = ProjectRootManager.getInstance(project).getContentRoots

    val cabalFiles = for {
      root <- projectRoots
      cabalFile <- findCabalFilesInVfs(root)
      ci <- PackageInfo.create(project, cabalFile.toNioPath.toFile)
    } yield ci

    if (cabalFiles.isEmpty) {
      HaskellNotificationGroup.logWarningBalloonEvent(project, s"No Cabal files found for project `${project.getName}`. Check your project configuration.")
    }
    cabalFiles
  }

  private def findCabalFilesInVfs(root: VirtualFile): Seq[VirtualFile] = {
    val result = scala.collection.mutable.ArrayBuffer[VirtualFile]()
    VfsUtil.processFileRecursivelyWithoutIgnored(root, (file: VirtualFile) => {
      if (!file.isDirectory && file.getName.endsWith(".cabal")) {
        result += file
      }
      true
    })
    result.toSeq
  }

  private def createComponentTargets(moduleCabalInfos: Iterable[PackageInfo]): Iterable[ComponentTarget] = {
    moduleCabalInfos.flatMap {
      case (cabalInfo: PackageInfo) => cabalInfo.cabalStanzas.map {
        case cs: LibraryCabalStanza => ComponentTarget(cs.modulePath, cs.packageName, cs.targetName, LibType, cs.sourceDirs, None, cs.isNoImplicitPreludeActive, cs.buildDepends, cs.exposedModuleNames)
        case cs: ExecutableCabalStanza => ComponentTarget(cs.modulePath, cs.packageName, cs.targetName, ExeType, cs.sourceDirs, cs.mainIs, cs.isNoImplicitPreludeActive, cs.buildDepends)
        case cs: TestSuiteCabalStanza => ComponentTarget(cs.modulePath, cs.packageName, cs.targetName, TestSuiteType, cs.sourceDirs, cs.mainIs, cs.isNoImplicitPreludeActive, cs.buildDepends)
        case cs: BenchmarkCabalStanza => ComponentTarget(cs.modulePath, cs.packageName, cs.targetName, BenchmarkType, cs.sourceDirs, cs.mainIs, cs.isNoImplicitPreludeActive, cs.buildDepends)
      }
    }
  }
}

private[external] class StackReplsManager(val project: Project) {

  private val globalRepl: GlobalStackRepl = GlobalStackRepl(project, HaskellSettingsState.getReplTimeout)
  private val globalRepl2: GlobalStackRepl = GlobalStackRepl(project, HaskellSettingsState.getReplTimeout)

  private val startedTargetProjectRepls = new ConcurrentHashMap[ProjectReplTargets, ProjectStackRepl]().asScala

  val modulePackageInfos: Iterable[PackageInfo] = StackReplsManager.createPackageInfos(project)

  val componentTargets: Iterable[ComponentTarget] = StackReplsManager.createComponentTargets(modulePackageInfos)

  val projectReplTargets: Iterable[ProjectReplTargets] = componentTargets.groupBy(_.stanzaType).flatMap { case (stanzaType, targets) =>
    if (stanzaType == LibType) {
      Seq(ProjectReplTargets(stanzaType, targets.toSeq))
    } else {
      targets.map(target => ProjectReplTargets(stanzaType, Seq(target)))
    }
  }

  def getRunningProjectRepls: Iterable[ProjectStackRepl] = {
    startedTargetProjectRepls.values.filter(_.available)
  }

  def libTargetsName: Option[String] = {
    projectReplTargets.find(_.stanzaType == LibType).map(_.targetsName)
  }

  def getGlobalRepl: GlobalStackRepl = globalRepl

  def getGlobalRepl2: GlobalStackRepl = globalRepl2

  def findProjectReplTargets(componentTarget: ComponentTarget): Option[ProjectReplTargets] = {
    projectReplTargets.find(_.targets.contains(componentTarget))
  }

  private def findProjectRepl(psiFile: PsiFile): Option[ProjectStackRepl] = {
    if (HaskellProjectUtil.isSourceFile(psiFile)) {
      val target = HaskellComponentsManager.findStackComponentInfo(psiFile)
      target.flatMap(findProjectReplTargets) match {
        case Some(t) => Some(getProjectRepl(t))
        case None =>
          HaskellNotificationGroup.warningEvent(project, s"No Haskell support for file `${psiFile.getName}` because no component target could be found for this file")
          None
      }
    } else {
      None
    }
  }

  private def getProjectRepl(targets: ProjectReplTargets): ProjectStackRepl = {
    startedTargetProjectRepls.get(targets) match {
      case Some(repl) => repl
      case None =>
        targets.synchronized {
          startedTargetProjectRepls.get(targets) match {
            case Some(r) => r
            case None =>
              val repl = createAndStartProjectRepl(targets)
              startedTargetProjectRepls.put(targets, repl)
              repl
          }
        }
    }
  }

  private def createAndStartProjectRepl(targets: ProjectReplTargets): ProjectStackRepl = {
    val repl = new ProjectStackRepl(project, targets, HaskellSettingsState.getReplTimeout)
    if (!project.isDisposed) {
      repl.start()
    }
    repl
  }
}
