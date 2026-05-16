/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.language

import com.intellij.psi.PsiFile
import me.fornever.haskeletor.core.language.HaskellImportOptimizerService
import me.fornever.haskeletor.editor.HaskellImportOptimizer

final class HaskellImportOptimizerServiceImpl extends HaskellImportOptimizerService {
  def removeRedundantImports(psiFile: PsiFile): Boolean = HaskellImportOptimizer.removeRedundantImports(psiFile)
}
