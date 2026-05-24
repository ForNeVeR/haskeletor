/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.psi.stubs.index

import com.intellij.psi.stubs.{StringStubIndexExtension, StubIndexKey}
import me.fornever.haskeletor.psi.HaskellNamedElement

object HaskellAllNameIndex:

  val Key: StubIndexKey[String, HaskellNamedElement] = StubIndexKey.createIndexKey("haskell.all.name")
  val Version = 1

class HaskellAllNameIndex extends StringStubIndexExtension[HaskellNamedElement]:

  override def getVersion: Int =
    super.getVersion + HaskellAllNameIndex.Version

  def getKey: StubIndexKey[String, HaskellNamedElement] =
    HaskellAllNameIndex.Key
