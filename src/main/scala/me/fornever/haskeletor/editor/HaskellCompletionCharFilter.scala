/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.editor

import com.intellij.codeInsight.lookup.CharFilter.Result
import com.intellij.codeInsight.lookup.{CharFilter, Lookup}
import me.fornever.haskeletor.HaskellFile

class HaskellCompletionCharFilter extends CharFilter:
  def acceptChar(c: Char, prefixLength: Int, lookup: Lookup): CharFilter.Result =
    if lookup == null || lookup.getPsiElement == null then return null
    val file = lookup.getPsiFile
    if !file.isInstanceOf[HaskellFile] then return null

    c match
      case ' ' |',' | ';' | ':' | '(' | ')' | '[' | ']' | '{' | '}' => Result.SELECT_ITEM_AND_FINISH_LOOKUP
      case _ => Result.ADD_TO_PREFIX
