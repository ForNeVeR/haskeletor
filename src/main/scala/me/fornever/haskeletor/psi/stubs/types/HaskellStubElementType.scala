/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.psi.stubs.types

import com.intellij.psi.stubs.{IStubElementType, IndexSink, StubElement}
import me.fornever.haskeletor.core.HaskellLanguage
import me.fornever.haskeletor.psi.HaskellCompositeElement

abstract class HaskellStubElementType[S <: StubElement[T], T <: HaskellCompositeElement](debugName: String) extends IStubElementType[S, T](debugName, HaskellLanguage.Instance) {

  def indexStub(stub: S, sink: IndexSink): Unit

  def getExternalId: String = {
    "haskell." + super.toString
  }
}
