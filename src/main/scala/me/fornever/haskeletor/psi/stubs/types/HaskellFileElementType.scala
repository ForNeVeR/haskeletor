/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.psi.stubs.types

import com.intellij.psi.stubs._
import com.intellij.psi.tree.IStubFileElementType
import com.intellij.psi.{PsiElement, PsiFile, StubBuilder}
import me.fornever.haskeletor.HaskellFile
import me.fornever.haskeletor.core.HaskellLanguage
import me.fornever.haskeletor.psi.stubs.types.HaskellFileElementType.HaskellFileStub

class HaskellFileElementType(language: HaskellLanguage) extends IStubFileElementType[HaskellFileStub](language) {
  override def indexStub(stub: HaskellFileStub, sink: IndexSink): Unit = {}

  private val Version: Int = 1

  override def getBuilder: StubBuilder = new DefaultStubBuilder() {
    override protected def createStubForFile(file: PsiFile): StubElement[_ <: PsiElement] = {
      file match {
        case f: HaskellFile => new HaskellFileStub(f)
        case _ => super.createStubForFile(file)
      }
    }
  }

  override def getStubVersion: Int = Version

  override def serialize(stub: HaskellFileStub, dataStream: StubOutputStream): Unit = {
  }

  override def deserialize(dataStream: StubInputStream, parentStub: StubElement[_ <: PsiElement]): HaskellFileStub = {
    new HaskellFileStub(null)
  }

  override def getExternalId: String = {
    "haskell.FILE"
  }
}

object HaskellFileElementType {
  type HaskellFileStub = PsiFileStubImpl[HaskellFile]
  val Instance = new HaskellFileElementType(HaskellLanguage.Instance)
}
