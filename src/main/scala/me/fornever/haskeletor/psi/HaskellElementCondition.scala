/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.psi

import com.intellij.openapi.util.Condition
import com.intellij.psi.PsiElement

object HaskellElementCondition:

  final val QualifiedNameElementCondition = new Condition[PsiElement]():
    override def value(psiElement: PsiElement): Boolean =
      psiElement match
        case _: HaskellQualifiedNameElement => true
        case _ => false

  final val DeclarationElementCondition = new Condition[PsiElement]():
    override def value(psiElement: PsiElement): Boolean =
      psiElement match
        case _: HaskellDeclarationElement => true
        case _ => false

  final val HighestDeclarationElementCondition = new Condition[PsiElement]():
    override def value(psiElement: PsiElement): Boolean =
      psiElement match
        case _: HaskellModuleDeclaration => true
        case e: HaskellDeclarationElement if e.getParent.getNode.getElementType == HaskellTypes.HS_TOP_DECLARATION => true
        case _ => false

  final val NamedElementCondition = new Condition[PsiElement]():
    override def value(psiElement: PsiElement): Boolean =
      psiElement match
        case _: HaskellNamedElement => true
        case _ => false
