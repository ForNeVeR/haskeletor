/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.core

import com.intellij.DynamicBundle
import org.jetbrains.annotations.{Nls, PropertyKey}

object HaskeletorBundle {
  private final val BUNDLE = "messages.HaskeletorBundle"
  private val instance = new HaskeletorBundle

  @Nls
  def message(@PropertyKey(resourceBundle = BUNDLE) key: String): String =
    //noinspection ReferencePassedToNls
    instance.getMessage(key)

  @Nls
  def message(@PropertyKey(resourceBundle = BUNDLE) key: String, param: Any): String =
    //noinspection ReferencePassedToNls
    instance.getMessage(key, param)

  @Nls
  def message(@PropertyKey(resourceBundle = BUNDLE) key: String, params: Any*): String =
    //noinspection ReferencePassedToNls
    instance.getMessage(key, params: _*)
}

class HaskeletorBundle extends DynamicBundle(HaskeletorBundle.BUNDLE)
