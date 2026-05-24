/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.core.project


case class GhcVersion(major: Int, minor: Int, patch: Int) extends Ordered[GhcVersion]:
  def compare(that: GhcVersion): Int = GhcVersion.versionOrdering.compare((major, minor, patch), (that.major, that.minor, that.patch))

  def prettyString: String =
    s"$major.$minor.$patch"

object GhcVersion:
  private val versionOrdering: Ordering[(Int, Int, Int)] = Ordering.Tuple3[Int, Int, Int]

  def parse(version: String): GhcVersion =
    val parts = version.split('.')
    GhcVersion(parts(0).toInt, parts(1).toInt, parts(2).toInt)
