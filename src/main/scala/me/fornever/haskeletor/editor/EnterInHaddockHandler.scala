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
import com.intellij.psi.{PsiDocumentManager, PsiFile}
import me.fornever.haskeletor.HaskellFile
import me.fornever.haskeletor.psi.HaskellTypes

class EnterInHaddockHandler extends EnterHandlerDelegateAdapter:

  override def preprocessEnter(file: PsiFile, editor: Editor, caretOffset: Ref[Integer], caretAdvance: Ref[Integer], dataContext: DataContext, originalHandler: EditorActionHandler): Result =
    if !file.isInstanceOf[HaskellFile] then return Result.Continue

    val document = editor.getDocument
    PsiDocumentManager.getInstance(file.getProject).commitDocument(document)

    val result = Option(caretOffset.get()).flatMap(offset => Option(file.findElementAt(offset)).map(element => {
      if element.getNode.getElementType == HaskellTypes.HS_HADDOCK then {
        document.insertString(offset, "-- ")
        caretAdvance.set(3)
        Result.Default
      } else {
        Result.Continue
      }
    }))
    result.getOrElse(Result.Continue)
