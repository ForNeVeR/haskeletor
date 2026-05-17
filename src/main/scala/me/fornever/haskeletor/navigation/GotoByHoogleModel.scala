/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.navigation

import com.intellij.BundleBase
import com.intellij.ide.IdeBundle
import com.intellij.ide.util.PropertiesComponent
import com.intellij.ide.util.gotoByName.{CustomMatcherModel, FilteringGotoByModel, LanguageRef}
import com.intellij.navigation.{ChooseByNameContributor, NavigationItem}
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.presentation.java.SymbolPresentationUtil
import me.fornever.haskeletor.core.HaskeletorBundle

import javax.swing.ListCellRenderer

class GotoByHoogleModel(val project: Project, val contributors: Array[ChooseByNameContributor]) extends FilteringGotoByModel[LanguageRef](project, contributors) with CustomMatcherModel {

  // Helping the Scala compiler to see that ListCellRenderer is parameterized by AnyRef
  override def getListCellRenderer: ListCellRenderer[_] = super.getListCellRenderer

  protected def filterValueFor(item: NavigationItem): LanguageRef = {
    null
  }

  def getPromptText: String = {
    "Hoogle for it"
  }

  def getCheckBoxName: String = {
    BundleBase.replaceMnemonicAmpersand("Include &non-&&project identifiers")
  }

  def getNotInMessage: String = {
    HaskeletorBundle.message("action.goto.no-matches-found", project.getName)
  }

  def getNotFoundMessage: String = {
    IdeBundle.message("label.no.matches.found")
  }

  def loadInitialCheckBoxState: Boolean = {
    val propertiesComponent: PropertiesComponent = PropertiesComponent.getInstance(myProject)
    true.toString == propertiesComponent.getValue("GoToClass.toSaveIncludeLibraries") && true.toString == propertiesComponent.getValue("GoToSymbol.includeLibraries")
  }

  def saveInitialCheckBoxState(state: Boolean): Unit = {
    val propertiesComponent: PropertiesComponent = PropertiesComponent.getInstance(myProject)
    if (true.toString == propertiesComponent.getValue("GoToClass.toSaveIncludeLibraries")) {
      propertiesComponent.setValue("GoToSymbol.includeLibraries", state.toString)
    }
  }

  def getFullName(element: Any): String = {
    Option(getElementName(element)).map(name => {
      element match {
        case e: PsiElement => SymbolPresentationUtil.getSymbolContainerText(e) + "." + name
        case _ => name
      }
    }).orNull
  }

  def getSeparators: Array[String] = {
    new Array[String](0)
  }

  def willOpenEditor: Boolean = {
    true
  }

  def matches(popupItem: String, userPattern: String): Boolean = {
    true
  }
}
