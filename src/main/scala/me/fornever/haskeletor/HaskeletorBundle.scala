/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

object HaskeletorBundle extends DynamicBundle(HaskeletorBundle.BUNDLE) {
  private final val BUNDLE = "messages.HaskeletorBundle"
  def message(@PropertyKey(resourceBundle = BUNDLE) key: String, params: Any*): String = getMessage(key, params: _*)
}
