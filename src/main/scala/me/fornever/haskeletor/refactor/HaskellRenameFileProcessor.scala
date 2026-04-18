/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.refactor

import com.intellij.psi.PsiElement
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import me.fornever.haskeletor.annotator.HaskellAnnotator
import me.fornever.haskeletor.psi.impl.HaskellPsiImplUtil
import me.fornever.haskeletor.psi.{HaskellModid, HaskellPsiUtil}
import me.fornever.haskeletor.util.ScalaUtil
import me.fornever.haskeletor.{HaskellFile, HaskellFileType}

import java.util

class HaskellRenameFileProcessor extends RenamePsiElementProcessor {

  override def canProcessElement(element: PsiElement): Boolean = {
    HaskellProjectUtil.isHaskellProject(element.getProject) && (element.isInstanceOf[HaskellFile] || element.isInstanceOf[HaskellModid])
  }

  override def prepareRenaming(psiElement: PsiElement, fileName: String, allRenames: util.Map[PsiElement, String]): Unit = {
    if (psiElement.isValid) {
      HaskellPsiUtil.findModuleDeclaration(psiElement.getContainingFile.getOriginalFile).foreach(moduleDeclaration => {
        moduleDeclaration.getModuleName.foreach(moduleName => {
          val newModuleName = HaskellRenameFileProcessor.createNewModuleName(moduleName, fileName)
          allRenames.put(moduleDeclaration.getModid, newModuleName)
          if (psiElement.isInstanceOf[HaskellModid]) {
            allRenames.put(psiElement.getContainingFile, newModuleName.split("\\.").last + "." + HaskellFileType.INSTANCE.getDefaultExtension)
          }
          super.prepareRenaming(psiElement, fileName, allRenames)
        })
      })
    }
  }

  override def getPostRenameCallback(element: PsiElement, newName: String, elementListener: RefactoringElementListener): Runnable = {
    ScalaUtil.runnable {
      val psiFile = element.getContainingFile.getOriginalFile
      HaskellPsiUtil.invalidateModuleName(psiFile)
      HaskellComponentsManager.clearLoadedModule(psiFile)
      HaskellAnnotator.restartDaemonCodeAnalyzerForFile(psiFile)
    }
  }
}

object HaskellRenameFileProcessor {

  def createNewModuleName(oldModuleName: String, fileName: String): String = {
    val conIds = oldModuleName.split("\\.")
    if (fileName.endsWith(HaskellFileType.INSTANCE.getDefaultExtension)) {
      conIds(conIds.length - 1) = HaskellPsiImplUtil.removeFileExtension(fileName)
    } else {
      val name = fileName.split("\\.").last
      conIds(conIds.length - 1) = name
    }
    conIds.mkString(".")
  }
}

