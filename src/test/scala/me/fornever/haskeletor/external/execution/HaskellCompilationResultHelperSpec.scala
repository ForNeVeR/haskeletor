/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.external.execution

import me.fornever.haskeletor.core.compiler.HaskellCompilationResultHelper
import org.junit.runner.RunWith
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class HaskellCompilationResultHelperSpec extends AnyFunSpec with Matchers with GivenWhenThen with BeforeAndAfterEach {

  describe("HaskellCompilationResultHelper") {
    describe("parseErrorLine") {
      it("should parse a standard error line") {
        Given("a standard compilation error line")
        val output = "/file/path/HaskellFile.hs:1:11:parse error on input     and so on"

        When("parsed to problem")
        val problem = HaskellCompilationResultHelper.parseErrorLine(output)

        Then("it should return Some with correct data")
        problem shouldBe a[Some[_]]
        val p = problem.get
        p.lineNr should equal(1)
        p.columnNr should equal(11)
        p.plainMessage should equal(s"parse error on input and so on")
      }

      it("should reject stack status lines with > operator") {
        Given("a stack status line like 'StateVar > build with ghc-9.0.2'")
        val output = "StateVar                         > build with ghc-9.0.2"

        When("parsed")
        val problem = HaskellCompilationResultHelper.parseErrorLine(output)

        Then("it should return None")
        problem should equal(None)
      }

      it("should reject stack progress lines") {
        Given("a stack progress line")
        val output = "Progress 0/67             StateVar                         > configure"

        When("parsed")
        val problem = HaskellCompilationResultHelper.parseErrorLine(output)

        Then("it should return None")
        problem should equal(None)
      }

      it("should handle invalid path characters gracefully") {
        Given("a line with illegal Windows path characters")
        val output = "file<with>chars:1:1:some error"

        When("parsed")
        val problem = HaskellCompilationResultHelper.parseErrorLine(output)

        Then("it should return None instead of throwing exception")
        problem should equal(None)
      }

      it("should parse Windows paths correctly") {
        Given("a Windows file path")
        val output = "C:\\Users\\project\\src\\Main.hs:5:2:error message"

        When("parsed")
        val problem = HaskellCompilationResultHelper.parseErrorLine(output)

        Then("it should return Some with correct file path")
        problem shouldBe a[Some[_]]
        val p = problem.get
        p.lineNr should equal(5)
        p.columnNr should equal(2)
        p.plainMessage should equal("error message")
      }

      it("should handle multiple spaces in error message") {
        Given("an error with multiple consecutive spaces")
        val output = "/path/file.hs:10:20:error with    many    spaces"

        When("parsed")
        val problem = HaskellCompilationResultHelper.parseErrorLine(output)

        Then("it should normalize the spaces")
        problem shouldBe a[Some[_]]
        problem.get.plainMessage should equal("error with many spaces")
      }
    }
  }
}
