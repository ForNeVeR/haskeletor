/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.util

import com.intellij.openapi.module.{Module, ModuleManager, ModuleType, ModuleUtilCore}
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots._
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.{VfsUtilCore, VirtualFile}
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.{PsiElement, PsiFile}
import com.intellij.util.PathUtilRt
import me.fornever.haskeletor.GlobalInfo
import me.fornever.haskeletor.core.project.GhcVersion
import me.fornever.haskeletor.external.component.HaskellComponentsManager
import me.fornever.haskeletor.module.HaskellModuleType
import me.fornever.haskeletor.sdk.HaskellSdkType
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes

import java.io.File
import scala.jdk.CollectionConverters._

object HaskellProjectUtil {

  final val Prelude = "Prelude"

  def setNoDiagnosticsShowCaretFlag(project: Project): Boolean = {
    HaskellComponentsManager.getGhcVersion(project).exists(ghcVersion =>
      ghcVersion >= GhcVersion(8, 2, 1)
    )
  }

  def isValidHaskellProject(project: Project, notifyNoSdk: Boolean): Boolean = {
    isHaskellProject(project) && HaskellSdkType.getStackPath(project, notifyNoSdk).isDefined
  }

  def isHaskellProject(project: Project): Boolean = {
    findProjectHaskellModules(project).nonEmpty
  }

  def isSourceFile(project: Project, virtualFile: VirtualFile): Boolean = {
    if (project.isDisposed) {
      // Well, it does not matter what is returned because is closing or closed
      false
    } else {
      val rootManager = ProjectRootManager.getInstance(project)
      val sourceRoots = rootManager.getModuleSourceRoots(JavaModuleSourceRootTypes.SOURCES).asScala.toSet.asJava
      VfsUtilCore.isUnder(virtualFile, sourceRoots)
    }
  }

  def isSourceFile(psiFile: PsiFile): Boolean = {
    val project = psiFile.getProject
    // Only source files can be only in memory
    HaskellFileUtil.findVirtualFile(psiFile).forall(vf => isSourceFile(project, vf))
  }

  def isLibraryFile(psiFile: PsiFile): Boolean = {
    val projectLibDirectory = getProjectLibrarySourcesDirectory(psiFile.getProject)
    HaskellFileUtil.findVirtualFile(psiFile).exists(vf => FileUtil.isAncestor(projectLibDirectory.getAbsolutePath, vf.getPath, true))
  }

  def getProjectLibrarySourcesDirectory(project: Project): File = {
    new File(GlobalInfo.getLibrarySourcesPath, project.getName)
  }

  def getModuleDir(module: Module): File = {
    val path = ModuleUtilCore.getModuleDirPath(module)
    val dir = new File(path)
    dir.getName match {
      case ".idea" => new File(PathUtilRt.getParentPath(path))
      case _ => dir
    }
  }

  def findCabalFiles(project: Project): Iterable[File] = {
    val modules = findProjectHaskellModules(project)
    val dirs = modules.map(getModuleDir)
    dirs.flatMap(findCabalFile)
  }

  def findCabalFile(directory: File): Option[File] = {
    directory.listFiles.find(_.getName.endsWith(".cabal"))
  }

  def findStackFile(directory: File): Option[File] = {
    directory.listFiles.find(_.getName == "stack.yaml")
  }

  def findPackageFiles(project: Project): Iterable[File] = {
    val modules = findProjectHaskellModules(project)
    val dirs = modules.map(getModuleDir)
    dirs.flatMap(findCabalFile)
    dirs.flatMap(findPackageFile)
  }

  def findStackFile(project: Project): Option[File] = {
    findStackFile(new File(project.getBasePath))
  }

  private def findPackageFile(directory: File): Option[File] = {
    directory.listFiles.find(_.getName == "package.yaml")
  }

  def getProjectSearchScope(project: Project): GlobalSearchScope = {
    GlobalSearchScope.allScope(project)
  }

  def getSearchScope(project: Project, includeNonProjectItems: Boolean): GlobalSearchScope = {
    if (includeNonProjectItems) getProjectSearchScope(project) else GlobalSearchScope.projectScope(project)
  }

  import ScalaUtil._

  def getProjectRootManager(project: Project): Option[ProjectRootManager] = {
    project.isDisposed.optionNot(Option(ProjectRootManager.getInstance(project))).flatten
  }

  def getModuleManager(project: Project): Option[ModuleManager] = {
    project.isDisposed.optionNot(Option(ModuleManager.getInstance(project))).flatten
  }

  def getModuleRootManager(project: Project, module: Module): Option[ModuleRootManager] = {
    project.isDisposed.optionNot(Option(ModuleRootManager.getInstance(module))).flatten
  }

  def findModule(psiElement: PsiElement): Option[Module] = {
    Option(ModuleUtilCore.findModuleForPsiElement(psiElement))
  }

  def findModuleForFile(psiFile: PsiFile): Option[Module] = {
    Option(ModuleUtilCore.findModuleForFile(psiFile))
  }

  def findProjectHaskellModules(project: Project): Iterable[Module] = {
    ModuleManager.getInstance(project).getModules.filter(ModuleType.get(_).isInstanceOf[HaskellModuleType])
  }

  def findProjectPackageNames(project: Project): Seq[String] = {
    HaskellComponentsManager.findProjectModulePackageNames(project).map(_._2)
  }
}

