/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.editor

import com.intellij.lang.CodeDocumentationAwareCommenter
import com.intellij.psi.PsiComment
import com.intellij.psi.tree.IElementType
import me.fornever.haskeletor.psi.HaskellTypes

class HaskellCommenter extends CodeDocumentationAwareCommenter:
  def getLineCommentPrefix: String =
    "--"

  def getBlockCommentPrefix: String =
    "{-"

  def getBlockCommentSuffix: String =
    "-}"

  def getCommentedBlockCommentPrefix: String =
    "{-"

  def getCommentedBlockCommentSuffix: String =
    "-}"

  def getLineCommentTokenType: IElementType =
    HaskellTypes.HS_COMMENT

  def getBlockCommentTokenType: IElementType =
    HaskellTypes.HS_NCOMMENT

  // Haskell documentation does not have similar syntax/structure as Javadoc so makes no sense to put some values here.
  def getDocumentationCommentTokenType: IElementType =
    null

  def getDocumentationCommentPrefix: String =
    null

  def getDocumentationCommentLinePrefix: String =
    null

  def getDocumentationCommentSuffix: String =
    null

  def isDocumentationComment(element: PsiComment): Boolean =
    element.getText.startsWith("-- |") || element.getText.startsWith("{-|")
