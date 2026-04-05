/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.psi

import com.intellij.navigation.{ItemPresentation, NavigationItem}
import me.fornever.haskeletor.psi.impl.HaskellPsiImplUtil

import java.util.logging.Logger

/**
 * Workaround: Grammar-Kit Gradle plugin doesn't support psiImplUtilClass,
 * so we provide default implementations that delegate to HaskellPsiImplUtil.
 */
trait HaskellDeclarationElement extends HaskellCompositeElement with NavigationItem {

  def getIdentifierElements: Iterable[HaskellNamedElement] = this match {
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
  }

  def getModuleName: Option[String] = this match {
    case md: HaskellModuleDeclaration => HaskellPsiImplUtil.getModuleName(md)
    case _ => HaskellPsiImplUtil.getModuleName(this)
  }

  override def getName: String = HaskellPsiImplUtil.getName(this)

  override def getPresentation: ItemPresentation = HaskellPsiImplUtil.getPresentation(this)
}
