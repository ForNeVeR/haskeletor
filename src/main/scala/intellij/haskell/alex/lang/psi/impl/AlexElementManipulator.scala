// SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
// SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
//
// SPDX-License-Identifier: Apache-2.0

package intellij.haskell.alex.lang.psi.impl

import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.Nullable

/**
  * @author ice1000
  */
class AlexElementManipulator extends AbstractElementManipulator[AlexHaskellCodeInjectionHost] {
  @Nullable
  @throws[IncorrectOperationException]
  override def handleContentChange(psi: AlexHaskellCodeInjectionHost,
                                   range: TextRange,
                                   newContent: String): AlexHaskellCodeInjectionHost = {
    val oldText = psi.getText
    val newText = oldText.substring(0, range.getStartOffset) + newContent + oldText.substring(range.getEndOffset)
    psi.updateText(newText)
  }
}

