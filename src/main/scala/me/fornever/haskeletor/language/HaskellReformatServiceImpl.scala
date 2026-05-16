/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.language

import com.intellij.psi.PsiFile
import me.fornever.haskeletor.action.OrmoluReformatAction
import me.fornever.haskeletor.core.language.HaskellReformatService

class HaskellReformatServiceImpl extends HaskellReformatService {
  override def reformat(psiFile: PsiFile): Boolean = OrmoluReformatAction.reformat(psiFile)
}
