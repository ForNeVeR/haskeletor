/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.refactor

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi._

class HaskellRefactoringSupportProvider extends RefactoringSupportProvider {

  override def isMemberInplaceRenameAvailable(psiElement: PsiElement, context: PsiElement): Boolean = {
    !psiElement.isInstanceOf[PsiFile] && isDefinedInProject(psiElement)
  }

  private def isDefinedInProject(psiElement: PsiElement) = {
    Option(psiElement.getReference).flatMap(x => Option(x.resolve)) match {
      case Some(e) => Option(e.getContainingFile).map(_.getOriginalFile).exists(pf => HaskellProjectUtil.isSourceFile(pf))
      case _ => false
    }
  }
}
