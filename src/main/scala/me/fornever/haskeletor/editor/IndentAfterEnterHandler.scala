/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.editor

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate.Result
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.util.Ref
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.{PsiDocumentManager, PsiElement, PsiFile, TokenType}
import me.fornever.haskeletor.HaskellFile
import me.fornever.haskeletor.psi.HaskellPsiUtil
import me.fornever.haskeletor.psi.HaskellTypes.*

class IndentAfterEnterHandler extends EnterHandlerDelegateAdapter:

  private val IndentTokenSet = TokenSet.create(HS_WHERE, HS_OF, HS_EQUAL, HS_IN, HS_DO, HS_IF, HS_THEN, HS_ELSE)

  override def preprocessEnter(file: PsiFile, editor: Editor, caretOffset: Ref[Integer], caretAdvance: Ref[Integer], dataContext: DataContext, originalHandler: EditorActionHandler): Result =
    if !file.isInstanceOf[HaskellFile] &&
      Option(caretOffset.get()).flatMap(offset => Option(file.findElementAt(offset))).exists(e => HaskellPsiUtil.findExpression(e).isEmpty) then return Result.Continue

    val document = editor.getDocument
    PsiDocumentManager.getInstance(file.getProject).commitDocument(document)

    val result = Option(caretOffset.get()).flatMap(offset => findNonWhiteSpaceElement(file, offset - 1).orElse(findNonWhiteSpaceElement(file, offset - 2)).map(element => {
      if IndentTokenSet.contains(element.getNode.getElementType) then {
        document.insertString(offset, "  ")
        caretAdvance.set(2)
        Result.Default
      } else {
        Result.Continue
      }
    })
    )
    result.getOrElse(Result.Continue)


  private def findNonWhiteSpaceElement(file: PsiFile, offset: Int): Option[PsiElement] =
    Option(file.findElementAt(offset - 1)).filterNot(_.getNode.getElementType == TokenType.WHITE_SPACE)
