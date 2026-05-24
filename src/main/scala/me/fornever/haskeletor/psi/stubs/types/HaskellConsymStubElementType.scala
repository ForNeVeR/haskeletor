/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.psi.stubs.types

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.{StubElement, StubInputStream}
import me.fornever.haskeletor.psi.HaskellConsym
import me.fornever.haskeletor.psi.impl.HaskellConsymImpl
import me.fornever.haskeletor.psi.stubs.HaskellConsymStub

class HaskellConsymStubElementType(debugName: String) extends HaskellNamedStubElementType[HaskellConsymStub, HaskellConsym](debugName):
  def createPsi(stub: HaskellConsymStub): HaskellConsym =
    new HaskellConsymImpl(stub, this)

  def createStub(psi: HaskellConsym, parentStub: StubElement[? <: PsiElement]): HaskellConsymStub =
    new HaskellConsymStub(parentStub, this, psi.getName)

  def deserialize(dataStream: StubInputStream, parentStub: StubElement[? <: PsiElement]): HaskellConsymStub =
    new HaskellConsymStub(parentStub, this, dataStream.readName)
