/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.psi.stubs.types

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.{IndexSink, NamedStubBase, StubOutputStream}
import me.fornever.haskeletor.psi.*
import me.fornever.haskeletor.psi.stubs.index.HaskellAllNameIndex

abstract class HaskellNamedStubElementType[S <: NamedStubBase[T], T <: HaskellNamedElement](debugName: String) extends HaskellStubElementType[S, T](debugName):

  def indexStub(stub: S, sink: IndexSink): Unit =
    val name: String = stub.getName
    if name != null then
      sink.occurrence(HaskellAllNameIndex.Key, name)

  def serialize(stub: S, dataStream: StubOutputStream): Unit =
    dataStream.writeName(stub.getName)

  override def shouldCreateStub(node: ASTNode): Boolean =
    node.getPsi match
      case _: HaskellVarid => true
      case _: HaskellVarsym => true
      case _: HaskellConid => true
      case _: HaskellConsym => true
      case _: HaskellModid => true
      case _ => false
