/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.{PsiElement, PsiReference}
import me.fornever.haskeletor.psi._
import org.jetbrains.annotations.Nullable

class HaskellCompositeElementImpl(node: ASTNode) extends ASTWrapperPsiElement(node) with HaskellCompositeElement {

  override def toString: String = {
    getNode.getElementType.toString
  }
}

abstract class HaskellExpressionElementImpl private[impl](node: ASTNode) extends HaskellCompositeElementImpl(node) with HaskellExpressionElement

abstract class HaskellNamedElementImpl private[impl](node: ASTNode) extends HaskellCompositeElementImpl(node) with HaskellNamedElement

/**
 * Mixin for qualifier elements (q_con_qualifier1-4, qualifier).
 * Provides runtime implementations of methods previously generated via psiImplUtilClass.
 */
abstract class HaskellQualifierElementImpl private[impl](node: ASTNode) extends HaskellNamedElementImpl(node) with HaskellQualifierElement {

  override def getName: String = getText

  override def getNameIdentifier: PsiElement = this

  override def setName(name: String): PsiElement = {
    this match {
      case _: HaskellQualifier =>
        val newName = HaskellPsiImplUtil.removeFileExtension(name)
        HaskellElementFactory.createQualifier(getProject, newName).foreach(replace)
      case _ =>
        HaskellElementFactory.createQConQualifier(getProject, name).foreach(replace)
    }
    this
  }

  @Nullable
  override def getReference: PsiReference = HaskellPsiImplUtil.getReference(this)

  override def getPresentation: ItemPresentation = HaskellPsiImplUtil.getPresentation(this)
}
