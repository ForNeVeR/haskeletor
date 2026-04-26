/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.cabal

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import me.fornever.haskeletor.core.cabal.CabalLanguage
import org.jetbrains.annotations.NotNull

import javax.swing._

class CabalFile(viewProvider: FileViewProvider) extends PsiFileBase(viewProvider, CabalLanguage.Instance) {

  @NotNull
  def getFileType: FileType = {
    CabalFileType.INSTANCE
  }

  override def toString: String = {
    "Cabal file"
  }

  override def getIcon(flags: Int): Icon = {
    super.getIcon(flags)
  }
}
