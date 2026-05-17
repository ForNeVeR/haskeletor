/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.icons;

import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public interface HaskellIcons {
  @NotNull Icon HaskellLogo = IconLoader.getIcon("/icons/haskell@16.png", HaskellIcons.class);
  @NotNull Icon HaskellFileLogo = IconLoader.getIcon("/icons/hs.png", HaskellIcons.class);
  @NotNull Icon HaskellSmallBlueLogo = IconLoader.getIcon("/icons/haskell-blue@16.png", HaskellIcons.class);

  @NotNull Icon Module = IconLoader.getIcon("/icons/module.png", HaskellIcons.class);
  @NotNull Icon Data = IconLoader.getIcon("/icons/data.png", HaskellIcons.class);
  @NotNull Icon NewType = IconLoader.getIcon("/icons/newtype.png", HaskellIcons.class);
  @NotNull Icon Type = IconLoader.getIcon("/icons/type.png", HaskellIcons.class);
  @NotNull Icon Class = IconLoader.getIcon("/icons/class.png", HaskellIcons.class);
  @NotNull Icon Default = IconLoader.getIcon("/icons/default_declaration.png", HaskellIcons.class);
  @NotNull Icon TypeFamily = IconLoader.getIcon("/icons/type_family.png", HaskellIcons.class);
  @NotNull Icon TypeInstance = IconLoader.getIcon("/icons/type_instance.png", HaskellIcons.class);
  @NotNull Icon TypeSignature = IconLoader.getIcon("/icons/type_signature.png", HaskellIcons.class);
  @NotNull Icon Instance = IconLoader.getIcon("/icons/instance.png", HaskellIcons.class);
  @NotNull Icon Foreign = IconLoader.getIcon("/icons/foreign.png", HaskellIcons.class);

  @NotNull Icon CabalLogo = IconLoader.getIcon("/icons/cabal.png", HaskellIcons.class);

  @NotNull Icon AlexLogo = IconLoader.getIcon("/icons/alex.png", HaskellIcons.class);
}
