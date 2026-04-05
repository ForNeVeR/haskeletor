/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.navigation

import com.intellij.ide.structureView._
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.pom.Navigatable
import com.intellij.psi.{PsiElement, PsiFile}
import me.fornever.haskeletor.HaskellFile
import me.fornever.haskeletor.icons.HaskellIcons
import me.fornever.haskeletor.psi.{HaskellDeclarationElement, HaskellPsiUtil}

import javax.swing.Icon

class HaskellStructureViewFactory extends PsiStructureViewFactory {
  def getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder = {
    new TreeBasedStructureViewBuilder {
      override def createStructureViewModel(editor: Editor): StructureViewModel = {
        new HaskellStructureViewModel(psiFile)
      }
    }
  }
}

private class HaskellStructureViewModel(psiFile: PsiFile) extends StructureViewModelBase(psiFile, new HaskellStructureViewTreeElement(psiFile, "")) with StructureViewModel.ElementInfoProvider {

  def isAlwaysShowsPlus(structureViewTreeElement: StructureViewTreeElement): Boolean = {
    false
  }

  def isAlwaysLeaf(structureViewTreeElement: StructureViewTreeElement): Boolean = {
    structureViewTreeElement.isInstanceOf[HaskellFile]
  }
}

private class HaskellStructureViewTreeElement(val element: PsiElement, val typeSignature: String) extends StructureViewTreeElement with ItemPresentation {

  def getValue: AnyRef = {
    element
  }

  override def navigate(requestFocus: Boolean): Unit = {
    element.asInstanceOf[Navigatable].navigate(requestFocus)
  }

  override def canNavigate: Boolean = {
    element.asInstanceOf[Navigatable].canNavigate
  }

  override def canNavigateToSource: Boolean = {
    element.asInstanceOf[Navigatable].canNavigateToSource
  }

  def getPresentation: ItemPresentation = {
    this
  }


  def getChildren: Array[TreeElement] = {
    (element match {
      case hf: HaskellFile => HaskellPsiUtil.findHaskellDeclarationElements(hf)
      case _ => Seq()
    }).map(declarationElement => new HaskellStructureViewTreeElement(declarationElement, declarationElement.getText)).toArray
  }

  override def getPresentableText: String = {
    element match {
      case hde: HaskellDeclarationElement => hde.getPresentation.getPresentableText
      case pf: PsiFile => pf.getName
      case _ => null
    }
  }

  override def getIcon(unused: Boolean): Icon = {
    element match {
      case hde: HaskellDeclarationElement => hde.getPresentation.getIcon(unused)
      case _: PsiFile => HaskellIcons.Module
      case _ => null
    }
  }

  override def getLocationString: String = null
}
