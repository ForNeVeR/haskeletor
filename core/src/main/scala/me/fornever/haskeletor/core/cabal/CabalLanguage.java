/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.core.cabal;

import com.intellij.lang.Language;
import org.jetbrains.annotations.NotNull;

public class CabalLanguage extends Language {

    public static final CabalLanguage Instance = new CabalLanguage();

    public CabalLanguage() {
        super("Cabal");
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Cabal";
    }

    @Override
    public boolean isCaseSensitive() {
        return true;
    }
}
