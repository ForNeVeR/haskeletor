/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import me.fornever.haskeletor.psi.*

import java.util.logging.Logger

abstract class HaskellDeclarationElementImpl(node: ASTNode) extends HaskellCompositeElementImpl(node)
  with HaskellDeclarationElement:

  def getIdentifierElements: Iterable[HaskellNamedElement] = this match
    case e: HaskellTypeSignature => HaskellPsiImplUtil.getIdentifierElements(e)
    case e: HaskellTypeDeclaration => HaskellPsiImplUtil.getIdentifierElements(e)
    case e: HaskellClassDeclaration => HaskellPsiImplUtil.getIdentifierElements(e)
    case e: HaskellInstanceDeclaration => HaskellPsiImplUtil.getIdentifierElements(e)
    case e: HaskellTypeFamilyDeclaration => HaskellPsiImplUtil.getIdentifierElements(e)
    case e: HaskellDerivingDeclaration => HaskellPsiImplUtil.getIdentifierElements(e)
    case e: HaskellTypeInstanceDeclaration => HaskellPsiImplUtil.getIdentifierElements(e)
    case e: HaskellDefaultDeclaration => HaskellPsiImplUtil.getIdentifierElements(e)
    case e: HaskellForeignDeclaration => HaskellPsiImplUtil.getIdentifierElements(e)
    case e: HaskellModuleDeclaration => HaskellPsiImplUtil.getIdentifierElements(e)
    case e: HaskellDataDeclaration => HaskellPsiImplUtil.getIdentifierElements(e)
    case e: HaskellNewtypeDeclaration => HaskellPsiImplUtil.getIdentifierElements(e)
    case _ =>
      Logger.getLogger(getClass.getName).warning(s"Unknown declaration element: $this")
      Seq.empty

  override def getName: String = HaskellPsiImplUtil.getName(this)

  override def getPresentation: ItemPresentation = HaskellPsiImplUtil.getPresentation(this)
