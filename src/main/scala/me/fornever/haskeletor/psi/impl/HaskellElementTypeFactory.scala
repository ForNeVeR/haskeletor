/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.psi.impl

import com.intellij.psi.tree.IElementType
import me.fornever.haskeletor.psi.stubs.types.*

object HaskellElementTypeFactory:

  def factory(name: String): IElementType =
    name match
      case "HS_CONID" => new HaskellConidStubElementType(name)
      case "HS_VARID" => new HaskellVaridStubElementType(name)
      case "HS_VARSYM" => new HaskellVarsymStubElementType(name)
      case "HS_CONSYM" => new HaskellConsymStubElementType(name)
      case "HS_MODID" => new HaskellModidStubElementType(name)
      case _ => throw new IllegalStateException(s"Unknown element name: $name")
