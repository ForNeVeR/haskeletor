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
import me.fornever.haskeletor.psi.HaskellModid
import me.fornever.haskeletor.psi.impl.HaskellModidImpl
import me.fornever.haskeletor.psi.stubs.HaskellModidStub

class HaskellModidStubElementType(debugName: String) extends HaskellNamedStubElementType[HaskellModidStub, HaskellModid](debugName):
  def createPsi(stub: HaskellModidStub): HaskellModid =
    new HaskellModidImpl(stub, this)

  def createStub(psi: HaskellModid, parentStub: StubElement[? <: PsiElement]): HaskellModidStub =
    new HaskellModidStub(parentStub, this, psi.getName)

  def deserialize(dataStream: StubInputStream, parentStub: StubElement[? <: PsiElement]): HaskellModidStub =
    new HaskellModidStub(parentStub, this, dataStream.readName)
