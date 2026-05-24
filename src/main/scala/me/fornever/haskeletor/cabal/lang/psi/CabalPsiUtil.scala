/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.cabal.lang.psi

import com.intellij.psi.PsiElement
import me.fornever.haskeletor.cabal.lang.psi
import me.fornever.haskeletor.psi.HaskellPsiUtil

object CabalPsiUtil:

  def getFieldContext(el: PsiElement): Option[psi.CabalFieldElement] =
    HaskellPsiUtil.collectFirstParent(el) { case el: psi.CabalFieldElement => el }
