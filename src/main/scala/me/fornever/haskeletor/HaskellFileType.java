/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor;

import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.LanguageFileType;
import me.fornever.haskeletor.core.HaskellLanguage;
import me.fornever.haskeletor.icons.HaskellIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class HaskellFileType extends LanguageFileType {

    public static final HaskellFileType INSTANCE = new HaskellFileType(HaskellLanguage.Instance);

    protected HaskellFileType(@NotNull Language language) {
        super(language);
    }

    @NotNull
    @Override
    public String getName() {
        return "Haskell";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Haskell file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "hs";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return HaskellIcons.HaskellLogo;
    }
}
