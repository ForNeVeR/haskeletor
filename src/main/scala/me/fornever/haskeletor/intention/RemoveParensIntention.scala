/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.intention

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import me.fornever.haskeletor.psi.HaskellPsiUtil
import me.fornever.haskeletor.psi.HaskellTypes.*

class RemoveParensIntention extends PsiElementBaseIntentionAction:

  override def invoke(project: Project, editor: Editor, psiElement: PsiElement): Unit =
    for
      (start, end) <- HaskellPsiUtil.getSelectionStartEnd(psiElement, editor)
    yield
      start.delete()
      end.delete()

  override def isAvailable(project: Project, editor: Editor, psiElement: PsiElement): Boolean =
    HaskellPsiUtil.getSelectionStartEnd(psiElement, editor) match
      case Some((start, end)) => psiElement.isWritable && start.getNode.getElementType == HS_LEFT_PAREN && end.getNode.getElementType == HS_RIGHT_PAREN
      case None => false

  override def getFamilyName: String = getText

  override def getText: String = "Remove parens around expression"
