/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import me.fornever.haskeletor.parser.HaskellParser;
import me.fornever.haskeletor.psi.HaskellTypes;
import me.fornever.haskeletor.psi.stubs.types.HaskellFileElementType$;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import static me.fornever.haskeletor.psi.HaskellTypes.*;

public class HaskellParserDefinition implements ParserDefinition {
  private static final IFileElementType FILE = HaskellFileElementType$.MODULE$.Instance();
  public static final TokenSet WhiteSpaces = TokenSet.create(TokenType.WHITE_SPACE);
  public static final TokenSet Comments = TokenSet.create(HS_COMMENT, HS_NCOMMENT, HS_HADDOCK, HS_NHADDOCK);
  public static final TokenSet PragmaStartEndIds = TokenSet.create(HS_PRAGMA_START, HS_PRAGMA_END);
  public static final TokenSet ReservedIdS = TokenSet.create(HS_CASE, HS_CLASS, HS_DATA, HS_DEFAULT, HS_DERIVING, HS_DO, HS_ELSE, HS_IF, HS_IMPORT,
    HS_IN, HS_INFIX, HS_INFIXL, HS_INFIXR, HS_INSTANCE, HS_LET, HS_MODULE, HS_NEWTYPE, HS_OF, HS_THEN, HS_TYPE, HS_WHERE, HS_UNDERSCORE);
  public static final TokenSet SpecialReservedIds = TokenSet.create(HS_TYPE_FAMILY, HS_FOREIGN_IMPORT, HS_FOREIGN_EXPORT, HS_TYPE_INSTANCE);
  public static final TokenSet AllReservedIds = TokenSet.orSet(ReservedIdS, SpecialReservedIds);
  public static final TokenSet ReservedOperators = TokenSet.create(HS_COLON_COLON, HS_EQUAL, HS_BACKSLASH, HS_VERTICAL_BAR, HS_LEFT_ARROW,
    HS_RIGHT_ARROW, HS_AT, HS_TILDE, HS_DOUBLE_RIGHT_ARROW, HS_DOT_DOT);
  public static final TokenSet Operators = TokenSet.orSet(ReservedOperators, TokenSet.create(HS_VARSYM_ID, HS_CONSYM_ID), TokenSet.create(HS_DOT));
  public static final TokenSet NumberLiterals = TokenSet.create(HS_DECIMAL, HS_FLOAT, HS_HEXADECIMAL, HS_OCTAL);
  public static final TokenSet SymbolsResOp = TokenSet.create(HS_EQUAL, HS_AT, HS_BACKSLASH, HS_VERTICAL_BAR, HS_TILDE);
  public static final TokenSet StringLiterals = TokenSet.create(HS_CHARACTER_LITERAL, HS_STRING_LITERAL);
  public static final TokenSet Literals = TokenSet.orSet(StringLiterals, NumberLiterals, TokenSet.create(HS_QUASIQUOTE));
  private static final HaskellParser HaskellParser = new HaskellParser();
  public static final TokenSet Ids = TokenSet.create(HS_VAR_ID, HS_CON_ID, HS_VARSYM_ID, HS_CONSYM_ID);

  @NotNull
  @Override
  public Lexer createLexer(Project project) {
    return new HaskellLexerAdapter();
  }

  @Override
  public @NonNull PsiParser createParser(Project project) {
    return HaskellParserDefinition.HaskellParser;
  }

  @Override
  public @NonNull IFileElementType getFileNodeType() {
    return FILE;
  }

  @NotNull
  @Override
  public TokenSet getWhitespaceTokens() {
    return HaskellParserDefinition.WhiteSpaces;
  }

  @NotNull
  @Override
  public TokenSet getCommentTokens() {
    return HaskellParserDefinition.Comments;
  }

  @NotNull
  @Override
  public TokenSet getStringLiteralElements() {
    return HaskellParserDefinition.StringLiterals;
  }

  @NotNull
  @Override
  public PsiElement createElement(ASTNode node) {
    return HaskellTypes.Factory.createElement(node);
  }

  @NotNull
  @Override
  public PsiFile createFile(@NonNull FileViewProvider viewProvider) {
    return new HaskellFile(viewProvider);
  }

  @Override
  public @NonNull SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right) {
    return SpaceRequirements.MAY;
  }
}
