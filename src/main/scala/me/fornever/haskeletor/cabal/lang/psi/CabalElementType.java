/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.cabal.lang.psi;

import com.intellij.psi.tree.IElementType;
import me.fornever.haskeletor.core.cabal.CabalLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class CabalElementType extends IElementType {
    public CabalElementType(@NotNull @NonNls String debugName) {
        super(debugName, CabalLanguage.Instance);
    }
}
