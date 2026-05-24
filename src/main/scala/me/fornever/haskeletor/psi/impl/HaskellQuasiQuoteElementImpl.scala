/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.{ElementManipulators, LiteralTextEscaper, PsiLanguageInjectionHost}
import me.fornever.haskeletor.psi.HaskellQuasiQuoteElement

abstract class HaskellQuasiQuoteElementImpl private[impl](node: ASTNode)
  extends HaskellCompositeElementImpl(node)
    with HaskellQuasiQuoteElement
    with PsiLanguageInjectionHost:
  override def isValidHost: Boolean =
    true

  override def updateText(text: String): HaskellQuasiQuoteElementImpl =
    ElementManipulators.handleContentChange(this, text)

  override def createLiteralTextEscaper(): LiteralTextEscaper[HaskellQuasiQuoteElementImpl] =
    LiteralTextEscaper.createSimple(this)
