/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.codeinsight

import com.intellij.codeInsight.hint.ImplementationTextSelectioner
import com.intellij.psi.PsiElement
import me.fornever.haskeletor.psi.{HaskellPsiUtil, HaskellTopDeclaration}

class HaskellImplementationTextSelectioner extends ImplementationTextSelectioner {

  override def getTextStartOffset(element: PsiElement): Int = {
    getTextRange(element)._1
  }

  override def getTextEndOffset(element: PsiElement): Int = {
    getTextRange(element)._2
  }

  private def getTextRange(element: PsiElement) = {
    HaskellPsiUtil.findTopDeclarationParent(element) match {
      case Some(dp) => Option(dp.getNextSibling) match {
        case Some(e: HaskellTopDeclaration) => (dp.getTextRange.getStartOffset, e.getTextRange.getEndOffset)
        case _ => Option(dp.getPrevSibling) match {
          case Some(e: HaskellTopDeclaration) => (e.getTextRange.getStartOffset, dp.getTextRange.getEndOffset)
          case _ => (dp.getTextRange.getStartOffset, dp.getTextRange.getEndOffset)
        }
      }
      case None => (element.getTextRange.getStartOffset, element.getTextRange.getEndOffset)
    }
  }

}
