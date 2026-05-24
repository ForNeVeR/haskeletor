/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.highlighter

import com.intellij.lang.{BracePair, PairedBraceMatcher}
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import me.fornever.haskeletor.HaskellParserDefinition.*
import me.fornever.haskeletor.psi.HaskellTypes.*

object HaskellBraceMatcher:


  private final val PAIRS = Array(
    new BracePair(HS_LEFT_PAREN, HS_RIGHT_PAREN, false),
    new BracePair(HS_PRAGMA_START, HS_PRAGMA_END, true),
    new BracePair(HS_LEFT_BRACE, HS_RIGHT_BRACE, true),
    new BracePair(HS_LEFT_BRACKET, HS_RIGHT_BRACKET, true)
  )

class HaskellBraceMatcher extends PairedBraceMatcher:
  def getPairs: Array[BracePair] = HaskellBraceMatcher.PAIRS

  def isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType): Boolean =
    !Ids.contains(contextType) && !Literals.contains(contextType) && contextType != HS_LEFT_PAREN && contextType != HS_LEFT_BRACE && contextType != HS_LEFT_BRACKET

  def getCodeConstructStart(file: PsiFile, openingBraceOffset: Int): Int =
    openingBraceOffset
