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
import me.fornever.haskeletor.psi.HaskellConid
import me.fornever.haskeletor.psi.impl.HaskellConidImpl
import me.fornever.haskeletor.psi.stubs.HaskellConidStub

class HaskellConidStubElementType(debugName: String) extends HaskellNamedStubElementType[HaskellConidStub, HaskellConid](debugName):
  def createPsi(stub: HaskellConidStub): HaskellConid =
    new HaskellConidImpl(stub, this)

  def createStub(psi: HaskellConid, parentStub: StubElement[? <: PsiElement]): HaskellConidStub =
    new HaskellConidStub(parentStub, this, psi.getName)

  def deserialize(dataStream: StubInputStream, parentStub: StubElement[? <: PsiElement]): HaskellConidStub =
    new HaskellConidStub(parentStub, this, dataStream.readName)
