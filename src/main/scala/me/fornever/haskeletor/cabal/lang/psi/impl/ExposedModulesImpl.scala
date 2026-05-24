/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.cabal.lang.psi.impl

import com.intellij.psi.PsiElement
import me.fornever.haskeletor.cabal.lang.psi.*
import me.fornever.haskeletor.psi.HaskellPsiUtil

trait ExposedModulesImpl extends PsiElement:

  def getModuleNames: Array[String] =
    HaskellPsiUtil.getChildOfType(this, classOf[ModuleList]) match
      case None => Array.empty
      case Some(moduleList) => HaskellPsiUtil.streamChildren(moduleList, classOf[Module]).map(c => c.getModuleName ).toArray
