// SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
// SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
//
// SPDX-License-Identifier: Apache-2.0

package intellij.haskell.alex

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

/**
  * @author ice1000
  */
class AlexFile(viewProvider: FileViewProvider) extends PsiFileBase(viewProvider, AlexLanguage.Instance) {

  def getFileType: FileType = {
    new AlexFileType(AlexLanguage.Instance)
  }

  override def toString: String = {
    " Alex file"
  }
}
