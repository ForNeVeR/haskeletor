/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.refactor

import com.intellij.lang.refactoring.NamesValidator
import com.intellij.openapi.project.Project
import me.fornever.haskeletor.{HaskellLexer, HaskellParserDefinition}

class HaskellNamesValidator extends NamesValidator:

  override def isKeyword(name: String, project: Project): Boolean =
    val lexer = new HaskellLexer
    lexer.start(name)
    HaskellParserDefinition.ReservedIdS.contains(lexer.getTokenType) ||
        HaskellParserDefinition.ReservedOperators.contains(lexer.getTokenType) ||
        HaskellParserDefinition.SymbolsResOp.contains(lexer.getTokenType)

  override def isIdentifier(name: String, project: Project): Boolean =
    !isKeyword(name, project) && !name.contains(' ')
