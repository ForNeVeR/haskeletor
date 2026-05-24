/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.cabal.lang.psi.impl

import com.intellij.psi.PsiElement

import scala.reflect.ClassTag

object CabalElementImplUtil:

  def assertUpCast[A <: PsiElement : ClassTag](el: PsiElement): A =
    val ct = implicitly[ClassTag[A]]
    el match
      case ct(x) => x
      case other =>
        throw new AssertionError(
          s"Expected ${ct.runtimeClass.getName} but got: " +
          s"${other.getClass.getName} (${other.getText})"
        )

