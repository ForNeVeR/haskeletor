/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.psi.impl

import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator
import com.intellij.util.IncorrectOperationException
import me.fornever.haskeletor.psi.HaskellElementFactory
import org.jetbrains.annotations.Nullable

/**
  * @author ice1000
  */
class HaskellQuasiQuoteManipulator extends AbstractElementManipulator[HaskellQuasiQuoteElementImpl]:
  @Nullable
  @throws[IncorrectOperationException]
  override def handleContentChange(psi: HaskellQuasiQuoteElementImpl,
                                   range: TextRange,
                                   newContent: String): HaskellQuasiQuoteElementImpl =
    val oldText = psi.getText
    val newText = oldText.substring(0, range.getStartOffset) + newContent + oldText.substring(range.getEndOffset)
    val newElement = HaskellElementFactory.createQuasiQuote(psi.getProject, newText)
    newElement.map(psi.replace(_).asInstanceOf[HaskellQuasiQuoteElementImpl]).orNull

  override def getRangeInElement(element: HaskellQuasiQuoteElementImpl): TextRange =
    val text = element.getText
    new TextRange(text.indexOf('|') + 1, text.lastIndexOf('|'))

