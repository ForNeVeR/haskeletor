/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.external.component

import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.{PsiElement, PsiFile}
import me.fornever.haskeletor.cabal.PackageInfo
import me.fornever.haskeletor.core.compiler.CompilationResult
import me.fornever.haskeletor.core.notifications.HaskellNotificationGroup
import me.fornever.haskeletor.core.project.{GhcVersion, GlobalProjectInfo}
import me.fornever.haskeletor.external.component.DefinitionLocationComponent.DefinitionLocationResult
import me.fornever.haskeletor.external.component.NameInfoComponentResult.NameInfoResult
import me.fornever.haskeletor.external.component.TypeInfoComponentResult.TypeInfoResult
import me.fornever.haskeletor.external.repl.StackRepl.StanzaType
import me.fornever.haskeletor.external.repl.StackReplsManager
import me.fornever.haskeletor.psi.HaskellPsiExtensions._
import me.fornever.haskeletor.psi.{HaskellPsiUtil, HaskellQualifiedNameElement}
import me.fornever.haskeletor.util.index.{HaskellFileIndex, HaskellModuleNameIndex}
import me.fornever.haskeletor.util.{ApplicationUtil, HaskellProjectUtil, ScalaFutureUtil}

import scala.concurrent._

object HaskellComponentsManager {

  case class ComponentTarget(module: Module, modulePath: String, packageName: String, target: String, stanzaType: StanzaType, sourceDirs: Seq[String],
                             mainIs: Option[String], isImplicitPreludeActive: Boolean, buildDepends: Seq[String], exposedModuleNames: Seq[String] = Seq.empty)

  def findModuleIdentifiersInCache(project: Project): Iterable[ModuleIdentifier] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val f = Future {
      blocking {
        BrowseModuleComponent.findModuleIdentifiersInCache(project)
      }
    }
    ScalaFutureUtil.waitForValue(project, f, "find module identifiers in cache") match {
      case Some(ids) => ids
      case None => Iterable()
    }
  }

  def clearLoadedModule(psiFile: PsiFile): Unit = {
    val projectRepl = StackReplsManager.getProjectRepl(psiFile)
    projectRepl.foreach(_.clearLoadedModule())
  }

  def findModuleIdentifiers(project: Project, moduleName: String)(implicit ec: ExecutionContext): Future[Option[Iterable[ModuleIdentifier]]] = {
    BrowseModuleComponent.findModuleIdentifiers(project, moduleName)
  }

  def findDefinitionLocation(psiFile: PsiFile, qualifiedNameElement: HaskellQualifiedNameElement, importQualifier: Option[String]): DefinitionLocationResult = {
    DefinitionLocationComponent.findDefinitionLocation(psiFile, qualifiedNameElement, importQualifier)
  }

  def findNameInfo(psiElement: PsiElement): NameInfoResult = {
    NameInfoComponent.findNameInfo(psiElement)
  }

  def findAvailableModuleNamesWithIndex(stackComponentInfo: ComponentTarget): Iterable[String] = {
    AvailableModuleNamesComponent.findAvailableModuleNamesWithIndex(stackComponentInfo)
  }

  def findAvailableModuleLibraryModuleNamesWithIndex(module: Module): Iterable[String] = {
    AvailableModuleNamesComponent.findAvailableModuleLibraryModuleNamesWithIndex(module)
  }

  def findStackComponentGlobalInfo(stackComponentInfo: ComponentTarget): Option[StackComponentGlobalInfo] = {
    StackComponentGlobalInfoComponent.findStackComponentGlobalInfo(stackComponentInfo)
  }

  def findStackComponentInfo(psiFile: PsiFile): Option[ComponentTarget] = {
    HaskellModuleInfoComponent.findComponentTarget(psiFile)
  }

  def findComponentTarget(project: Project, filePath: String): Option[ComponentTarget] = {
    HaskellModuleInfoComponent.findComponentTarget(project, filePath)
  }

  def getGlobalProjectInfo(project: Project): Option[GlobalProjectInfo] = {
    GlobalProjectInfoComponent.findGlobalProjectInfo(project)
  }

  def getSupportedLanguageExtension(project: Project): Iterable[String] = {
    GlobalProjectInfoComponent.findGlobalProjectInfo(project).map(_.supportedLanguageExtensions).getOrElse(Iterable())
  }

  def getGhcVersion(project: Project): Option[GhcVersion] = {
    GlobalProjectInfoComponent.findGlobalProjectInfo(project).map(_.ghcVersion)
  }

  def getAvailableStackagePackages(project: Project): Iterable[String] = {
    GlobalProjectInfoComponent.findGlobalProjectInfo(project).map(_.availableStackagePackageNames).getOrElse(Iterable())
  }

  def findProjectPackageNames(project: Project): Option[Iterable[String]] = {
    StackReplsManager.getReplsManager(project).map(_.modulePackageInfos.map { case (_, ci) => ci.packageName })
  }

  def findCabalInfos(project: Project): Iterable[PackageInfo] = {
    StackReplsManager.getReplsManager(project).map(_.modulePackageInfos.map { case (_, ci) => ci }).getOrElse(Iterable())
  }

  def loadHaskellFile(psiFile: PsiFile, fileModified: Boolean): Option[CompilationResult] = {
    LoadComponent.load(psiFile, fileModified)
  }

  def invalidateFileInfos(psiFile: PsiFile): Unit = {
    HaskellModuleInfoComponent.invalidate(psiFile)
  }

  def findProjectModulePackageNames(project: Project): Seq[(Module, String)] = {
    findStackComponentInfos(project).map(info => (info.module, info.packageName)).distinct
  }

  def invalidateDefinitionLocations(project: Project): Unit = {
    DefinitionLocationComponent.invalidate(project)
  }

  def findLibraryPackageInfos(project: Project): Seq[LibraryPackageInfo] = {
    LibraryPackageInfoComponent.libraryPackageInfos(project).toSeq
  }

  def invalidateBrowseInfo(project: Project, moduleNames: Seq[String]): Unit = {
    BrowseModuleComponent.invalidateModuleNames(project, moduleNames)
  }

  def findStackComponentInfos(project: Project): Seq[ComponentTarget] = {
    StackReplsManager.getReplsManager(project).map(_.componentTargets.toSeq).getOrElse(Seq())
  }

  def invalidateGlobalCaches(project: Project): Unit = {
    HaskellNotificationGroup.logInfoEvent(project, "Start to invalidate cache")
    GlobalProjectInfoComponent.invalidate(project)
    LibraryPackageInfoComponent.invalidate(project)
    HaskellModuleInfoComponent.invalidate(project)
    BrowseModuleComponent.invalidate(project)
    NameInfoComponent.invalidateAll(project)
    DefinitionLocationComponent.invalidateAll(project)
    TypeInfoComponent.invalidateAll(project)
    HaskellPsiUtil.invalidateAllModuleNames(project)
    LibraryPackageInfoComponent.invalidate(project)
    HaskellModuleNameIndex.invalidate(project)
    FileModuleIdentifiers.invalidateAll(project)
    StackComponentGlobalInfoComponent.invalidate(project)
    HaskellNotificationGroup.logInfoEvent(project, "Finished with invalidating cache")
  }

  def preloadLibraryIdentifiersCaches(project: Project): Unit = {
    HaskellNotificationGroup.logInfoEvent(project, "Start to preload library identifiers cache")
    preloadLibraryIdentifiers(project)
    HaskellNotificationGroup.logInfoEvent(project, "Finished with preloading library identifiers cache")
  }

  def preloadAllLibraryIdentifiersCaches(project: Project): Unit = {
    HaskellNotificationGroup.logInfoEvent(project, "Start to preload all library identifiers cache")
    preloadAllLibraryIdentifiers(project)
    HaskellNotificationGroup.logInfoEvent(project, "Finished with preloading all library identifiers cache")
  }

  def preloadStackComponentInfoCache(project: Project): Unit = {
    HaskellNotificationGroup.logInfoEvent(project, "Start to preload stack component info cache")
    preloadStackComponentInfos(project)
    HaskellNotificationGroup.logInfoEvent(project, "Finished with preloading stack component info cache")
  }

  def preloadLibraryFilesCache(project: Project): Unit = {
    HaskellNotificationGroup.logInfoEvent(project, "Start to preload library files cache")
    preloadLibraryFiles(project)
    HaskellNotificationGroup.logInfoEvent(project, "Finished with preloading library files cache")
  }

  def findTypeInfoForElement(psiElement: PsiElement): TypeInfoResult = {
    TypeInfoComponent.findTypeInfoForElement(psiElement)
  }

  def findTypeInfoForSelection(psiFile: PsiFile, selectionModel: SelectionModel): TypeInfoResult = {
    TypeInfoComponent.findTypeInfoForSelection(psiFile, selectionModel)
  }

  private def preloadStackComponentInfos(project: Project): Unit = {
    if (!project.isDisposed) {
      findStackComponentInfos(project).foreach { info =>
        findStackComponentGlobalInfo(info)
        val projectModuleNames = AvailableModuleNamesComponent.findAvailableProjectModuleNames(info)
        HaskellModuleNameIndex.fillCache(project, projectModuleNames)
      }
    }
  }

  private def preloadLibraryFiles(project: Project): Unit = {
    if (!project.isDisposed) {
      val libraryPackageInfos = LibraryPackageInfoComponent.libraryPackageInfos(project)
      HaskellModuleNameIndex.fillCache(project, libraryPackageInfos.flatMap(libraryModuleNames => libraryModuleNames.exposedModuleNames ++ libraryModuleNames.hiddenModuleNames))
    }
  }

  private def preloadLibraryIdentifiers(project: Project): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global

    if (!project.isDisposed) {
      BrowseModuleComponent.findModuleIdentifiers(project, HaskellProjectUtil.Prelude)
    }

    if (!project.isDisposed) {
      val projectHaskellFiles = ApplicationUtil.runReadActionWithFileAccess(project, HaskellFileIndex.findProjectHaskellFiles(project), "Find Haskell project files").toOption.getOrElse(Iterable())

      val componentInfos = projectHaskellFiles.flatMap(f => HaskellComponentsManager.findStackComponentInfo(f)).toSeq.distinct

      val importedLibraryModuleNames =
        projectHaskellFiles.flatMap(f => {
          if (project.isDisposed) {
            Iterable()
          } else {
            val packageInfos = componentInfos.flatMap(HaskellComponentsManager.findStackComponentGlobalInfo).flatMap(_.packageInfos)

            val exposedLibraryModuleNames = packageInfos.flatMap(_.exposedModuleNames).distinct
            val importDeclarations = ApplicationUtil.runReadActionWithFileAccess(project, HaskellPsiUtil.findImportDeclarations(f), "In preloadLibraryIdentifiers findImportDeclarations").toOption.getOrElse(Iterable())
            importDeclarations.flatMap(id => ApplicationUtil.runReadAction(id.getModuleName, Some(project))).filter(mn => exposedLibraryModuleNames.contains(mn)).filterNot(_ == HaskellProjectUtil.Prelude)
          }
        })

      if (!project.isDisposed) {
        if (StackReplsManager.getGlobalRepl(project).exists(_.available)) {
          importedLibraryModuleNames.toSeq.distinct.foreach(mn => {
            if (!project.isDisposed) {
              BrowseModuleComponent.findModuleIdentifiersSync(project, mn)
            }
          })
        }
      }
    }
  }

  private def preloadAllLibraryIdentifiers(project: Project): Unit = {
    if (!project.isDisposed) {
      val componentInfos = findStackComponentInfos(project)
      val packageInfos = componentInfos.flatMap(info => findStackComponentGlobalInfo(info).map(_.packageInfos).getOrElse(Seq())).distinct

      if (!project.isDisposed) {
        if (StackReplsManager.getGlobalRepl(project).exists(_.available)) {
          packageInfos.flatMap(_.exposedModuleNames).distinct.foreach(mn => {
            if (!project.isDisposed) {
              BrowseModuleComponent.findModuleIdentifiersSync(project, mn)
              // We have to wait for other requests which have more priority because those are on dispatch thread
              Thread.sleep(100)
            }
          })
        }
      }
    }
  }
}
