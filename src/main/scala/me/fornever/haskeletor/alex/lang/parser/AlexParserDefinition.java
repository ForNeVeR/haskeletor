/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.alex.lang.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import me.fornever.haskeletor.alex.AlexFile;
import me.fornever.haskeletor.alex.AlexLanguage;
import me.fornever.haskeletor.alex.lang.lexer.AlexLexer;
import me.fornever.haskeletor.alex.lang.psi.AlexTypes;
import org.jetbrains.annotations.NotNull;

public class AlexParserDefinition implements ParserDefinition {

    public static final IFileElementType FILE = new IFileElementType(AlexLanguage.Instance);

    public static final TokenSet STRINGS = TokenSet.create(AlexTypes.ALEX_STRING);

    @Override
    public @NotNull Lexer createLexer(Project project) {
        return new AlexLexer();
    }

    @Override
    public @NotNull PsiParser createParser(Project project) {
        return new AlexParser();
    }

    @Override
    public @NotNull IFileElementType getFileNodeType() {
        return AlexParserDefinition.FILE;
    }

    @Override
    public @NotNull TokenSet getCommentTokens() {
        return TokenSet.EMPTY;
    }

    @Override
    public @NotNull TokenSet getStringLiteralElements() {
        return AlexParserDefinition.STRINGS;
    }

    @Override
    public @NotNull PsiElement createElement(ASTNode astNode) {
        return AlexTypes.Factory.createElement(astNode);
    }

    @Override
    public @NotNull PsiFile createFile(@NotNull FileViewProvider fileViewProvider) {
        return new AlexFile(fileViewProvider);
    }
}
