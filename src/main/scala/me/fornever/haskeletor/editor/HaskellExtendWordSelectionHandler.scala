/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.editor

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.tree.IElementType
import com.intellij.psi.{PsiComment, PsiElement, PsiWhiteSpace}
import me.fornever.haskeletor.HaskellFile
import me.fornever.haskeletor.psi.HaskellPsiUtil
import me.fornever.haskeletor.psi.HaskellTypes.*

import java.util
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.*

class HaskellExtendWordSelectionHandler extends ExtendWordSelectionHandler:

  override def canSelect(e: PsiElement): Boolean =
    Option(e.getContainingFile).exists(_.isInstanceOf[HaskellFile]) && !e.isInstanceOf[PsiComment] && !e.isInstanceOf[PsiWhiteSpace]

  override def select(e: PsiElement, editorText: CharSequence, cursorOffset: Int, editor: Editor): util.List[TextRange] =
    val startOffset = e.getTextRange.getStartOffset
    val nextEndOffsets = getOffsets(Some(e), ListBuffer.empty[(Int, IElementType)], (e: PsiElement) => e.getNextSibling, (e: PsiElement) => (e.getTextRange.getEndOffset, e.getNode.getElementType))

    val prevStartOffsets = getOffsets(Option(e.getPrevSibling), ListBuffer.empty[(Int, IElementType)], (e: PsiElement) => e.getPrevSibling, (e: PsiElement) => (e.getTextRange.getStartOffset, e.getNode.getElementType))
    val lastEndOffset = nextEndOffsets.lastOption.getOrElse((e.getTextRange.getEndOffset, e.getNode.getElementType))

    val allSelectOptions = nextEndOffsets.map(eo => (e.getNode.getElementType, eo._2, new TextRange(startOffset, eo._1))) ++ prevStartOffsets.map(so => (so._2, lastEndOffset._2, new TextRange(so._1, lastEndOffset._1)))

    allSelectOptions.filter(x =>
      if x._1 == HS_LEFT_PAREN then {
        x._2 == HS_RIGHT_PAREN
      } else if x._1 == HS_LEFT_BRACE then {
        x._2 == HS_RIGHT_BRACE
      } else if x._1 == HS_LEFT_BRACKET then {
        x._2 == HS_RIGHT_BRACKET
      } else if x._2 == HS_RIGHT_PAREN then {
        x._1 == HS_LEFT_PAREN
      } else if x._2 == HS_RIGHT_BRACE then {
        x._1 == HS_LEFT_BRACE
      } else if x._2 == HS_RIGHT_BRACKET then {
        x._1 == HS_LEFT_BRACKET
      } else {
        true
      }
    ).map(_._3).asJava

  private def getOffsets(element: Option[PsiElement], offsets: ListBuffer[(Int, IElementType)], getSibling: PsiElement => PsiElement, getOffset: PsiElement => (Int, IElementType)): ListBuffer[(Int, IElementType)] =
    def recur(e: PsiElement): ListBuffer[(Int, IElementType)] =
      getOffsets(Option(getSibling(e)), offsets, getSibling, getOffset)

    element match
      case Some(e) =>
        HaskellPsiUtil.findQualifiedName(e) match
          case None => e match
            case e: PsiWhiteSpace => recur(e)
            case e: PsiElement if e.getNode.getElementType == HS_COMMA => recur(e)
            case e: PsiElement if e.getNode.getElementType == HS_NEWLINE | e.getNode.getElementType == HS_EQUAL | e.getNode.getElementType == HS_LEFT_ARROW => offsets
            case _ =>
              offsets += getOffset(e)
              recur(e)
          case Some(qe) =>
            offsets += getOffset(qe)
            recur(qe)
      case None => offsets
