/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.core.project

import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class GhcVersionSpec extends AnyFlatSpec with Matchers {
  "GhcVersion.compare" should "order versions lexicographically" in {
    GhcVersion(9, 2, 1) should be > GhcVersion(9, 2, 0)
    GhcVersion(9, 2, 1) should be > GhcVersion(9, 1, 9)
    GhcVersion(9, 2, 1) should be > GhcVersion(8, 10, 7)
    GhcVersion(8, 10, 7) should be < GhcVersion(9, 0, 1)
    GhcVersion(9, 2, 1) shouldEqual GhcVersion(9, 2, 1)
  }

  it should "support ordered operators without recursion" in {
    noException should be thrownBy GhcVersion(9, 0, 0).compare(GhcVersion(8, 10, 7))
    GhcVersion(8, 10, 7) >= GhcVersion(8, 10, 7) shouldBe true
    GhcVersion(8, 10, 7) >= GhcVersion(8, 10, 6) shouldBe true
    GhcVersion(8, 10, 7) <= GhcVersion(8, 10, 8) shouldBe true
  }
}
