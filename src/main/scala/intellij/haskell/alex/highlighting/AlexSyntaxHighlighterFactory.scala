// SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
// SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
//
// SPDX-License-Identifier: Apache-2.0

package intellij.haskell.alex.highlighting

import com.intellij.openapi.fileTypes.{SyntaxHighlighter, SyntaxHighlighterFactory}
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
  * @author ice1000
  */
class AlexSyntaxHighlighterFactory extends SyntaxHighlighterFactory {
  override def getSyntaxHighlighter(project: Project, virtualFile: VirtualFile): SyntaxHighlighter = {
    new AlexSyntaxHighlighter
  }
}
