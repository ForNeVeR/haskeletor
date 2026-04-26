/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes._
import com.intellij.psi.FileViewProvider
import me.fornever.haskeletor.core.HaskellLanguage
import org.jetbrains.annotations.NotNull

import javax.swing._

class HaskellFile(viewProvider: FileViewProvider) extends PsiFileBase(viewProvider, HaskellLanguage.Instance) {

  @NotNull
  def getFileType: FileType = {
    HaskellFileType.INSTANCE
  }

  override def toString: String = {
    "Haskell file"
  }

  override def getIcon(flags: Int): Icon = {
    super.getIcon(flags)
  }
}
