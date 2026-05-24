/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.highlighter

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import me.fornever.haskeletor.HaskellLexer
import me.fornever.haskeletor.psi.HaskellTypes.*
import org.jetbrains.annotations.NotNull

//noinspection TypeAnnotation
object HaskellSyntaxHighlighter:
  final val Illegal = createTextAttributesKey("HS_ILLEGAL", DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE)
  final val Comment = createTextAttributesKey("HS_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
  final val BlockComment = createTextAttributesKey("HS_NCOMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT)
  final val DocComment = createTextAttributesKey("HS_HADDOCK", DefaultLanguageHighlighterColors.DOC_COMMENT)
  final val BlockDocComment = createTextAttributesKey("HS_NHADDOCK", DefaultLanguageHighlighterColors.DOC_COMMENT)
  final val String = createTextAttributesKey("HS_STRING", DefaultLanguageHighlighterColors.STRING)
  final val Number = createTextAttributesKey("HS_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
  final val Keyword = createTextAttributesKey("HS_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
  final val FunctionName = createTextAttributesKey("HS_FUNCTION_NAME", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION)
  final val Parentheses = createTextAttributesKey("HS_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES)
  final val Brace = createTextAttributesKey("HSL_BRACE", DefaultLanguageHighlighterColors.BRACES)
  final val Bracket = createTextAttributesKey("HS_BRACKET", DefaultLanguageHighlighterColors.BRACKETS)
  final val Variable = createTextAttributesKey("HS_VARIABLE", DefaultLanguageHighlighterColors.LOCAL_VARIABLE)
  final val PragmaContent = createTextAttributesKey("HS_PRAGMA_CONTENT", DefaultLanguageHighlighterColors.IDENTIFIER)
  final val Constructor = createTextAttributesKey("HS_CONSTRUCTOR", DefaultLanguageHighlighterColors.INSTANCE_FIELD)
  final val Operator = createTextAttributesKey("HS_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
  final val ReservedSymbol = createTextAttributesKey("HS_SYMBOL", DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL)
  final val Pragma = createTextAttributesKey("HS_PRAGMA", DefaultLanguageHighlighterColors.METADATA)
  final val Quasiquote = createTextAttributesKey("HS_QUASI_QUOTES", DefaultLanguageHighlighterColors.METADATA)
  final val Default = createTextAttributesKey("HS_DEFAULT", DefaultLanguageHighlighterColors.LOCAL_VARIABLE)

class HaskellSyntaxHighlighter extends SyntaxHighlighterBase:

  import com.intellij.openapi.fileTypes.SyntaxHighlighterBase.*
  import me.fornever.haskeletor.HaskellParserDefinition.*

  @NotNull
  def getHighlightingLexer: Lexer =
    new HaskellLexer

  @NotNull
  def getTokenHighlights(elementType: IElementType): Array[TextAttributesKey] =
    import me.fornever.haskeletor.highlighter.HaskellSyntaxHighlighter.*

    elementType match
      case TokenType.BAD_CHARACTER => pack(Illegal)
      case et if PragmaStartEndIds.contains(et) => pack(Pragma)
      case et if et == HS_COMMENT => pack(Comment)
      case et if et == HS_HADDOCK | et == HS_NHADDOCK => pack(DocComment)
      case et if et == HS_NCOMMENT => pack(BlockComment)
      case et if et == HS_STRING_LITERAL | et == HS_CHARACTER_LITERAL => pack(String)
      case et if NumberLiterals.contains(et) => pack(Number)
      case et if et == HS_LEFT_PAREN | et == HS_RIGHT_PAREN => pack(Parentheses)
      case et if et == HS_LEFT_BRACE | et == HS_RIGHT_BRACE => pack(Brace)
      case et if et == HS_LEFT_BRACKET | et == HS_RIGHT_BRACKET => pack(Bracket)
      case et if AllReservedIds.contains(et) => pack(Keyword)
      case et if SymbolsResOp.contains(et) => pack(ReservedSymbol)
      case et if Operators.contains(et) => pack(Operator)
      case et if et == HS_VAR_ID => pack(Variable)
      case et if et == HS_CON_ID => pack(Constructor)
      case et if et == HS_MODID => pack(Constructor)
      case et if et == HS_QUASIQUOTE => pack(Quasiquote)
      case et if WhiteSpaces.contains(et) | et == HS_NEWLINE => pack(null)
      case _ => pack(Default)
