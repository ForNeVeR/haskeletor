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
import me.fornever.haskeletor.psi.HaskellVarsym
import me.fornever.haskeletor.psi.impl.HaskellVarsymImpl
import me.fornever.haskeletor.psi.stubs.HaskellVarsymStub

class HaskellVarsymStubElementType(debugName: String) extends HaskellNamedStubElementType[HaskellVarsymStub, HaskellVarsym](debugName) {
  def createPsi(stub: HaskellVarsymStub): HaskellVarsym = {
    new HaskellVarsymImpl(stub, this)
  }

  def createStub(psi: HaskellVarsym, parentStub: StubElement[? <: PsiElement]): HaskellVarsymStub = {
    new HaskellVarsymStub(parentStub, this, psi.getName)
  }

  def deserialize(dataStream: StubInputStream, parentStub: StubElement[? <: PsiElement]): HaskellVarsymStub = {
    new HaskellVarsymStub(parentStub, this, dataStream.readName)
  }
}
