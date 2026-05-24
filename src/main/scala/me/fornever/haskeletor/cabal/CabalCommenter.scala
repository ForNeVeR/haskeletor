/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.cabal

import com.intellij.lang.Commenter

class CabalCommenter extends Commenter:
  override def getCommentedBlockCommentPrefix: String = null

  override def getBlockCommentSuffix: String = null

  override def getBlockCommentPrefix: String = null

  override def getLineCommentPrefix: String = "--"

  override def getCommentedBlockCommentSuffix: String = null
