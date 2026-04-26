/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.psi.impl

import com.intellij.openapi.util.TextRange
import com.intellij.psi.{AbstractElementManipulator, PsiFileFactory}
import com.intellij.util.IncorrectOperationException
import me.fornever.haskeletor.core.HaskellLanguage
import org.jetbrains.annotations.Nullable

/**
  * @author ice1000
  */
class HaskellStringLiteralManipulator extends AbstractElementManipulator[HaskellStringLiteralElementImpl] {
  @Nullable
  @throws[IncorrectOperationException]
  override def handleContentChange(psi: HaskellStringLiteralElementImpl,
                                   range: TextRange,
                                   newContent: String): HaskellStringLiteralElementImpl = {
    val oldText = psi.getText
    val newText = oldText.substring(0, range.getStartOffset) + newContent + oldText.substring(range.getEndOffset)
    val newElement = PsiFileFactory
      .getInstance(psi.getProject)
      .createFileFromText("a.hs", HaskellLanguage.Instance, newText, false, false)
      .getLastChild
      .getLastChild
    psi.replace(newElement).asInstanceOf[HaskellStringLiteralElementImpl]
  }

  override def getRangeInElement(element: HaskellStringLiteralElementImpl): TextRange = {
    new TextRange(1, element.getTextLength - 1)
  }
}

