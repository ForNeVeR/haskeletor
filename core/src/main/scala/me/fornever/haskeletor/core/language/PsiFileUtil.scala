/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.core.language

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.{PsiFile, PsiManager}

object PsiFileUtil {

  def convertToHaskellFileDispatchThread(project: Project, virtualFile: VirtualFile): Option[PsiFile] = {
    findCachedPsiFile(project, virtualFile) match {
      case pf@Some(_) => pf
      case None => findPsiFile(project, virtualFile)
    }
  }

  def findCachedPsiFile(project: Project, virtualFile: VirtualFile): Option[PsiFile] = {
    val manager = PsiManagerEx.getInstanceEx(project)
    val fileManager = manager.getFileManager
    ProgressManager.checkCanceled()
    Option(fileManager.getCachedPsiFile(virtualFile))
  }

  def findPsiFile(project: Project, virtualFile: VirtualFile): Option[PsiFile] = {
    Option(PsiManager.getInstance(project).findFile(virtualFile))
  }
}
