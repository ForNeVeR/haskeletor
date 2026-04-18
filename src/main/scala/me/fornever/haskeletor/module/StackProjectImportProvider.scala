/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.module

import com.intellij.ide.util.projectWizard.{ModuleWizardStep, WizardContext}
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

import java.io.File

class StackProjectImportProvider extends ProjectImportProvider(new StackProjectImportBuilder) {

  override def createSteps(context: WizardContext): Array[ModuleWizardStep] =
    Array(new HaskellModuleWizardStep(context, HaskellModuleType.getInstance.createModuleBuilder()))

  override def canImport(fileOrDirectory: VirtualFile, project: Project): Boolean = {
    val file = new File(HaskellFileUtil.getAbsolutePath(fileOrDirectory))
    if (file.isDirectory) {
      file.listFiles().map(_.getName).contains("stack.yaml")
    } else {
      false
    }
  }

  override def getFileSample: String = "<b>Haskell</b> project file (stack.yaml)"
}
