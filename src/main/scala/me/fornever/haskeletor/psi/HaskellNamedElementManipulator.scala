/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator

class HaskellNamedElementManipulator extends AbstractElementManipulator[HaskellNamedElement]:
  def handleContentChange(psi: HaskellNamedElement, range: TextRange, newContent: String): HaskellNamedElement =
    psi.setName(newContent)
    psi
