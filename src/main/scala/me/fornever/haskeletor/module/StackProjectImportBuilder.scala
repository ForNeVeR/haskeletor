/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.module

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.{ModifiableModuleModel, Module, ModuleType}
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import me.fornever.haskeletor.icons.HaskellIcons
import me.fornever.haskeletor.stackyaml.StackYamlComponent
import me.fornever.haskeletor.util.{ApplicationUtil, ScalaUtil}

import java.io.File
import java.util
import javax.swing.Icon

class StackProjectImportBuilder extends ProjectImportBuilder[AnyRef] {
  override def getName: String = "Haskell Stack"

  override def getList: util.List[AnyRef] = new util.ArrayList[AnyRef]()

  override def getIcon: Icon = HaskellIcons.HaskellLogo

  override def setOpenProjectSettingsAfter(on: Boolean): Unit = {}

  override def setList(list: util.List[AnyRef]): Unit = ()

  override def isMarked(element: AnyRef): Boolean = true

  override def getTitle: String = "Stack project importer"

  override def commit(project: Project, model: ModifiableModuleModel, modulesProvider: ModulesProvider, artifactModel: ModifiableArtifactModel): java.util.List[Module] = {

    val packagePaths = StackProjectImportBuilder.getPackagePaths(project)

    packagePaths.foreach(packageRelativePath => {
      StackProjectImportBuilder.addHaskellModule(project, packageRelativePath, getFileToImport)
    })

    if (!packagePaths.contains(StackProjectImportBuilder.projectRootRelativePath)) {
      val parentModuleBuilder = new ParentModuleBuilder(project)
      parentModuleBuilder.setModuleFilePath(new File(project.getBasePath, project.getName + "-parent.iml").getPath)
      parentModuleBuilder.setName("Parent module")
      parentModuleBuilder.commit(project)
      parentModuleBuilder.addModuleConfigurationUpdater((_: Module, rootModel: ModifiableRootModel) => {
        parentModuleBuilder.setupRootModel(rootModel)
      })
    }

    HaskellProjectUtil.getModuleManager(project).map(_.getModules).getOrElse(Array()).toList.asJava
  }
}

object StackProjectImportBuilder {

  private final val projectRootRelativePath = "."

  def addHaskellModule(project: Project, packageRelativePath: String, projectRoot: String): Unit = {
    val moduleBuilder = HaskellModuleType.getInstance.createModuleBuilder()
    val moduleDirectory = HaskellModuleBuilder.getModuleRootDirectory(packageRelativePath, projectRoot)
    if (moduleDirectory.exists()) {
      ApplicationUtil.runReadAction(HaskellModuleBuilder.createCabalInfo(project, projectRoot, packageRelativePath), Some(project)) match {
        case Some(cabalInfo) =>
          val packageName = cabalInfo.packageName
          moduleBuilder.setCabalInfo(cabalInfo)
          moduleBuilder.setName(packageName)
          moduleBuilder.setModuleFilePath(getModuleImlFilePath(moduleDirectory, packageName))
          ApplicationManager.getApplication.invokeAndWait(ScalaUtil.runnable(moduleBuilder.commit(project)))
          moduleBuilder.addModuleConfigurationUpdater((_: Module, rootModel: ModifiableRootModel) => {
            moduleBuilder.setupRootModel(rootModel)
          })
        case None =>
          Messages.showInfoMessage(project, s"Can not add package $packageRelativePath as module because ${moduleDirectory.getAbsolutePath} does not contain Cabal file or Cabal file can not be parsed", "Adding module failed")
      }
    } else {
      Messages.showInfoMessage(project, s"Can not add package $packageRelativePath as module because it's absolute file path ${moduleDirectory.getAbsolutePath} does not exist.", "Adding module failed")
    }
  }

  def getModuleImlFilePath(moduleDirectory: File, packageName: String): String = {
    new File(moduleDirectory, packageName + ".iml").getAbsolutePath
  }

  def getPackagePaths(project: Project): Seq[String] = {
    StackYamlComponent.getPackagePaths(project).getOrElse(Seq(projectRootRelativePath))
  }
}

class ParentModuleBuilder(val project: Project) extends ModuleBuilder {
  override def isOpenProjectSettingsAfter = true

  override def canCreateModule = false

  override def setupRootModel(modifiableRootModel: ModifiableRootModel): Unit = {
    modifiableRootModel.addContentEntry(HaskellFileUtil.getUrlByPath(project.getBasePath))

    val stackWorkDirectory = HaskellModuleBuilder.getStackWorkDirectory(this)
    stackWorkDirectory.mkdir()
    Option(LocalFileSystem.getInstance.refreshAndFindFileByIoFile(stackWorkDirectory)).foreach(f => {
      val contentEntry = doAddContentEntry(modifiableRootModel)
      contentEntry.addExcludeFolder(f)
    })
  }

  override def getModuleType: ModuleType[_ <: ModuleBuilder] = HaskellModuleType.getInstance

  override def getPresentableName = "Parent Module"

  override def getGroupName: String = getPresentableName

  override def isTemplateBased = true

  override def getDescription = "Module at root of project so directories at root level are accessible"
}
