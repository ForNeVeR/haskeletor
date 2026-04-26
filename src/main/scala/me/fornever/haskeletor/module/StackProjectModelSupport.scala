/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.module

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.ui.Messages
import me.fornever.haskeletor.stackyaml.StackYamlComponent
import me.fornever.haskeletor.util.{ApplicationUtil, ScalaUtil}

import java.io.File

object StackProjectModelSupport {

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
