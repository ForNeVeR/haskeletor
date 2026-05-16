/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.editor

import com.intellij.codeInsight.template.{TemplateActionContext, TemplateContextType}
import com.intellij.psi.util.PsiTreeUtil
import me.fornever.haskeletor.HaskellFileType
import me.fornever.haskeletor.psi.{HaskellExpression, HaskellFileHeader}

class HaskellTemplateContextType extends TemplateContextType("Haskell") {
  override def isInContext(templateActionContext: TemplateActionContext): Boolean =
    templateActionContext.getFile.getFileType == HaskellFileType.INSTANCE
}

class HaskellPragmaTemplateContextType extends TemplateContextType("Pragma") {
  override def isInContext(templateActionContext: TemplateActionContext): Boolean = {
    val file = templateActionContext.getFile
    val offset = templateActionContext.getStartOffset
    if (file.getFileType != HaskellFileType.INSTANCE) return false
    if (offset < 5) return true
    val element = file.findElementAt(offset - 5)
    element != null &&
      PsiTreeUtil.getParentOfType(element, classOf[HaskellFileHeader]) != null
  }
}

class HaskellGlobalDefinitionTemplateContextType extends TemplateContextType("Global definition") {
  override def isInContext(templateActionContext: TemplateActionContext): Boolean = {
    val file = templateActionContext.getFile
    val offset = templateActionContext.getStartOffset
    if (file.getFileType != HaskellFileType.INSTANCE) return false
    var element = file.findElementAt(offset)
    if (element == null) element = file.findElementAt(offset - 1)
    element != null &&
      PsiTreeUtil.getParentOfType(element, classOf[HaskellExpression]) == null
  }
}
