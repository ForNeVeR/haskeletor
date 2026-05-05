/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.settings;

import com.intellij.openapi.options.Configurable;

/**
 * Abstract Java base class for {@link HaskellConfigurable}.
 *
 * <p>Scala 2 emits forwarding bytecode for every unoverridden Java interface default method,
 * which causes the plugin verifier to flag {@code Configurable.getDisplayNameFast()} as
 * internal API use. By placing the {@code implements Configurable} on a Java class, no such
 * bytecode is generated; Scala then extends this class (not the interface) and is never
 * triggered to produce forwarding methods.
 */
public abstract class HaskellConfigurableBase implements Configurable {
}
