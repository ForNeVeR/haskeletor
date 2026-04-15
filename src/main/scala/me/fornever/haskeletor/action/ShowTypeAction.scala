/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.action

import com.intellij.openapi.actionSystem.{ActionUpdateThread, AnAction, AnActionEvent}
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.{PsiElement, PsiFile, TokenType}
import me.fornever.haskeletor.external.component.{FileModuleIdentifiers, HaskellComponentsManager, StackProjectManager}
import me.fornever.haskeletor.psi.HaskellTypes.HS_NEWLINE
import me.fornever.haskeletor.psi._
import me.fornever.haskeletor.util.{HaskellEditorUtil, StringUtil}

class ShowTypeAction extends AnAction {

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  override def update(actionEvent: AnActionEvent): Unit = {
    HaskellEditorUtil.enableAction(onlyForSourceFile = true, actionEvent)
  }

  def actionPerformed(actionEvent: AnActionEvent): Unit = {
    if (!StackProjectManager.isInitializing(actionEvent.getProject)) {
      ActionUtil.findActionContext(actionEvent).foreach(actionContext => {
        val editor = actionContext.editor
        val psiFile = actionContext.psiFile

        actionContext.selectionModel match {
          case Some(sm) => HaskellComponentsManager.findTypeInfoForSelection(psiFile, sm) match {
            case Right(info) => HaskellEditorUtil.showHint(editor, StringUtil.escapeString(info.typeSignature))
            case _ => HaskellEditorUtil.showHint(editor, "Could not determine type for selection")
          }
          case _ => ()
            Option(psiFile.findElementAt(editor.getCaretModel.getOffset)).filterNot(e => e.getNode.getElementType == HS_NEWLINE || e.getNode.getElementType == TokenType.WHITE_SPACE).orElse(Option(psiFile.findElementAt(editor.getCaretModel.getOffset - 1))).foreach { psiElement =>
              ShowTypeAction.showTypeAsHint(actionContext.project, editor, psiElement, psiFile)
            }
        }
      })
    }
    else {
      HaskellEditorUtil.showHaskellSupportIsNotAvailableWhileInitializing(actionEvent.getProject)
    }
  }
}

object ShowTypeAction {

  def showTypeAsHint(project: Project, editor: Editor, psiElement: PsiElement, psiFile: PsiFile, sticky: Boolean = false): Unit = {
    showTypeInfo(project, editor, psiElement, psiFile, sticky = sticky)
  }

  private def showTypeInfo(project: Project, editor: Editor, psiElement: PsiElement, psiFile: PsiFile, sticky: Boolean = false): Unit = {
    showTypeSignatureAsHint(project, editor, sticky, getTypeInfo(psiFile, psiElement))
  }

  private def getTypeInfo(psiFile: PsiFile, psiElement: PsiElement): String = {
    HaskellComponentsManager.findTypeInfoForElement(psiElement) match {
      case Right(info) => info.typeSignature
      case Left(noInfo) =>
        findTypeSignatureFromScope(psiFile, psiElement) match {
          case Some(typeSignature) => typeSignature
          case None => s"Could not determine type for `${psiElement.getText}` | ${noInfo.message}"
        }
    }
  }

  private def showTypeSignatureAsHint(project: Project, editor: Editor, sticky: Boolean, typeSignature: String): Unit = {
    HaskellEditorUtil.showHint(editor, StringUtil.escapeString(typeSignature), sticky)
  }

  private def findTypeSignatureFromScope(psiFile: PsiFile, psiElement: PsiElement) = {
    if (HaskellPsiUtil.findExpression(psiElement).isDefined) {
      HaskellPsiUtil.findQualifiedName(psiElement).flatMap(qualifiedNameElement => {
        val definedInFile = HaskellComponentsManager.findDefinitionLocation(psiFile, qualifiedNameElement, None).toOption.map(_.namedElement.getContainingFile)
        if (definedInFile.contains(psiFile)) {
          // To prevent stale type info while compilation errors
          None
        } else {
          val name = qualifiedNameElement.getName
          val declaration = FileModuleIdentifiers.findAvailableModuleIdentifiers(psiFile).find(_.name == name).map(_.declaration)
          declaration.orElse(HaskellPsiUtil.findHaskellDeclarationElements(psiFile).find(_.getIdentifierElements.exists(_.getName == name)).map(_.getText.replaceAll("""\s+""", " ")))
        }
      })
    } else {
      None
    }
  }
}
