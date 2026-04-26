/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.cabal.lang.parser;

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
import me.fornever.haskeletor.cabal.CabalFile;
import me.fornever.haskeletor.cabal.lang.lexer.CabalParsingLexer;
import me.fornever.haskeletor.cabal.lang.psi.CabalElementFactory;
import me.fornever.haskeletor.cabal.lang.psi.CabalTypes;
import me.fornever.haskeletor.core.cabal.CabalLanguage;
import org.jetbrains.annotations.NotNull;

public class CabalParserDefinition implements ParserDefinition {

  // NOTE: You must use CabalLanguage.INSTANCE instead of Language.findInstance()
  // since the language may not have been initialized yet.
  public static final IFileElementType FILE = new IFileElementType(CabalLanguage.Instance);

  @NotNull
  @Override
  public Lexer createLexer(Project project) {
    return new CabalParsingLexer();
  }

  @Override
  public PsiParser createParser(Project project) {
    return new CabalParser();
  }

  @Override
  public IFileElementType getFileNodeType() {
    return FILE;
  }

  @NotNull
  @Override
  public TokenSet getWhitespaceTokens() {
    return TokenSet.create(TokenType.WHITE_SPACE);
  }

  @NotNull
  @Override
  public TokenSet getCommentTokens() {
    return TokenSet.create(CabalTypes.COMMENT);
  }

  @NotNull
  @Override
  public TokenSet getStringLiteralElements() {
    return TokenSet.EMPTY;
  }

  @NotNull
  @Override
  public PsiElement createElement(ASTNode node) {
    return CabalElementFactory.createElement(node);
  }

  @Override
  public PsiFile createFile(FileViewProvider viewProvider) {
    return new CabalFile(viewProvider);
  }

  @Override
  public SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right) {
    return SpaceRequirements.MAY;
  }
}
