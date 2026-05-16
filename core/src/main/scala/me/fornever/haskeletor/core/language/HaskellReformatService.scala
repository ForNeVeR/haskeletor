/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.core.language

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiFile

trait HaskellReformatService {
  def reformat(psiFile: PsiFile): Boolean
}

object HaskellReformatService {
  def getInstance(): HaskellReformatService = ApplicationManager.getApplication.getService(classOf[HaskellReformatService])
}
