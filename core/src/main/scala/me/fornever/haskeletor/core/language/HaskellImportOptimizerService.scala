/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.core.language

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiFile

trait HaskellImportOptimizerService {
  def removeRedundantImports(psiFile: PsiFile): Boolean
}

object HaskellImportOptimizerService {
  def getInstance(): HaskellImportOptimizerService = ApplicationManager.getApplication.getService(classOf[HaskellImportOptimizerService])
}
