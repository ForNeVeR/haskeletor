/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.alex.lang.parser

import com.intellij.lang.{ASTNode, ParserDefinition, PsiParser}
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.tree.{IFileElementType, TokenSet}
import com.intellij.psi.{FileViewProvider, PsiElement, PsiFile}
import me.fornever.haskeletor.alex.lang.lexer.AlexLexer
import me.fornever.haskeletor.alex.lang.psi.AlexTypes
import me.fornever.haskeletor.alex.{AlexFile, AlexLanguage}

/**
  * @author ice1000
  */
object AlexParserDefinition {
  final val FILE = new IFileElementType(AlexLanguage.Instance)

  final val STRINGS = TokenSet.create(AlexTypes.ALEX_STRING)
}

/**
  * @author ice1000
  */
class AlexParserDefinition extends ParserDefinition {
  override def createLexer(project: Project): Lexer = {
    new AlexLexer
  }

  override def createParser(project: Project): PsiParser = {
    new AlexParser
  }

  override def getFileNodeType: IFileElementType = {
    AlexParserDefinition.FILE
  }

  override def getCommentTokens: TokenSet = {
    TokenSet.EMPTY
  }

  override def getStringLiteralElements: TokenSet = {
    AlexParserDefinition.STRINGS
  }

  override def createElement(astNode: ASTNode): PsiElement = {
    AlexTypes.Factory.createElement(astNode)
  }

  override def createFile(fileViewProvider: FileViewProvider): PsiFile = {
    new AlexFile(fileViewProvider)
  }
}
