/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.alex.lang

import com.intellij.lang.{BracePair, PairedBraceMatcher}
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import me.fornever.haskeletor.alex.lang.psi.AlexTypes

/**
  * @author ice1000
  */
object AlexBraceMatcher:
  final val PAIRS = Array(
    new BracePair(AlexTypes.ALEX_SOMETHING_IS_GONNA_HAPPEN, AlexTypes.ALEX_SOMETHING_HAS_ALREADY_HAPPENED, false),
    new BracePair(AlexTypes.ALEX_STATEFUL_TOKENS_RULE_START, AlexTypes.ALEX_STATEFUL_TOKENS_RULE_END, false),
    new BracePair(AlexTypes.ALEX_LEFT_LISP, AlexTypes.ALEX_RIGHT_LISP, false)
  )

/**
  * @author ice1000
  */
class AlexBraceMatcher extends PairedBraceMatcher:
  override def getPairs: Array[BracePair] =
    AlexBraceMatcher.PAIRS

  override def isPairedBracesAllowedBeforeType(iElementType: IElementType, iElementType1: IElementType): Boolean =
    true

  override def getCodeConstructStart(psiFile: PsiFile, openingBraceOffset: Int): Int =
    openingBraceOffset
