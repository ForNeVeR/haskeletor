/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.cabal.lang.psi.impl

import com.intellij.psi.PsiElement
import me.fornever.haskeletor.cabal.lang.psi.CabalTypes
import me.fornever.haskeletor.psi.HaskellPsiUtil

trait MainIsImpl extends PsiElement:

  def getValue: Option[String] =
    HaskellPsiUtil.getChildNodes(this, CabalTypes.FREEFORM).headOption.map(_.getText)
