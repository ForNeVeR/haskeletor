/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.psi.impl

import com.intellij.lang.ASTNode
import me.fornever.haskeletor.psi.{HaskellNamedElement, HaskellQName, HaskellQualifiedNameElement}

abstract class HaskellQualifiedNameElementImpl(node: ASTNode) extends HaskellCompositeElementImpl(node)
  with HaskellQualifiedNameElement
  with HaskellQName {

  override def getName: String = HaskellPsiImplUtil.getName(this)

  def getIdentifierElement: HaskellNamedElement = HaskellPsiImplUtil.getIdentifierElement(this)

  def getQualifierName: Option[String] = HaskellPsiImplUtil.getQualifierName(this)
}
