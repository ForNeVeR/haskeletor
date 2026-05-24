/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.psi

import com.intellij.navigation.NavigationItem
import me.fornever.haskeletor.psi.impl.HaskellPsiImplUtil

trait HaskellDeclarationElement extends HaskellCompositeElement with NavigationItem:

  def getIdentifierElements: Iterable[HaskellNamedElement]

  def getModuleName: Option[String] = this match
    case md: HaskellModuleDeclaration => HaskellPsiImplUtil.getModuleName(md)
    case _ => HaskellPsiImplUtil.getModuleName(this)
