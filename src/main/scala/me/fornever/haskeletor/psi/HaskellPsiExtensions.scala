/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.psi

import me.fornever.haskeletor.psi.impl.HaskellPsiImplUtil

/**
 * Extension methods for generated PSI interfaces that lack psiImplUtilClass support.
 * Import `HaskellPsiExtensions._` at call sites that need these methods.
 */
object HaskellPsiExtensions:

  implicit class HaskellImportDeclarationOps(val decl: HaskellImportDeclaration) extends AnyVal:
    def getModuleName: Option[String] = HaskellPsiImplUtil.getModuleName(decl)
