/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.settings

sealed abstract class HTool extends Product with Serializable {
  def name: String
}

object HTool {

  case object Hlint extends HTool {
    def name: String = "hlint"
  }

  case object Hoogle extends HTool {
    def name: String = "hoogle"
  }

  case object StylishHaskell extends HTool {
    def name: String = "stylish-haskell"
  }

  case object Ormolu extends HTool {
    def name: String = "ormolu"

  }

}
