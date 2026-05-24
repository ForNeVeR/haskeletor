/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.util

import com.intellij.openapi.editor.Document
import com.intellij.openapi.vfs.VirtualFile

case class LineColumnPosition(lineNr: Int, columnNr: Int) extends Ordered[LineColumnPosition]:

  def compare(that: LineColumnPosition): Int =
    val lineNrCompare = this.lineNr compare that.lineNr
    if lineNrCompare == 0 then
      this.columnNr compare that.columnNr
    else
      lineNrCompare

object LineColumnPosition:

  def fromOffset(virtualFile: VirtualFile, offset: Int): Option[LineColumnPosition] =
    for
      doc <- HaskellFileUtil.findDocument(virtualFile)
      li <- if offset <= doc.getTextLength then Some(doc.getLineNumber(offset)) else None
    yield LineColumnPosition(li + 1, offset - doc.getLineStartOffset(li) + 1)

  def getOffset(virtualFile: VirtualFile, lineColPos: LineColumnPosition): Option[Int] =
    for
      doc <- HaskellFileUtil.findDocument(virtualFile)
      lineIndex <- getLineIndex(lineColPos.lineNr, doc)
      startOffsetLine = doc.getLineStartOffset(lineIndex)
    yield startOffsetLine + lineColPos.columnNr - 1

  private def getLineIndex(lineNr: Int, doc: Document) =
    if lineNr > doc.getLineCount then
      None
    else
      Some(lineNr - 1)
