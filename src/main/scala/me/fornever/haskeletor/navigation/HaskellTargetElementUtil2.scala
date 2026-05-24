/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.navigation

import com.intellij.codeInsight.TargetElementEvaluatorEx2
import com.intellij.psi.PsiElement
import me.fornever.haskeletor.psi.HaskellNamedElement

class HaskellTargetElementUtil2 extends TargetElementEvaluatorEx2:

  override def getNamedElement(element: PsiElement): PsiElement =
    if element.isInstanceOf[HaskellNamedElement] then
      element
    else
      null

  override def isAcceptableNamedParent(parent: PsiElement): Boolean =
    false
