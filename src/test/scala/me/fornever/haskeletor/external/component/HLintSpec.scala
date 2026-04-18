/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package external

import org.junit.runner.RunWith
import org.scalatest.GivenWhenThen
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class HLintSpec extends AnyFunSpec with Matchers with GivenWhenThen {

  describe("HLint component") {
    it("should parse HLint output") {
      Given("output of HLint")
      val output =
        """
           [{"module":["Hello113"],"decl":["testa"],"severity":"Warning","hint":"Redundant bracket","file":"/testproject/src/Hello113.hs","startLine":7,"startColumn":9,"endLine":7,"endColumn":15,"from":"(\"aa\")","to":"\"aa\"","note":["redundant parens"],"refactorings":"[Replace {rtype = Expr, pos = SrcSpan {startLine = 7, startCol = 9, endLine = 7, endCol = 15}, subts = [(\"x\",SrcSpan {startLine = 7, startCol = 10, endLine = 7, endCol = 14})], orig = \"x\"}]"}
           ,{"module":["Hello113"],"decl":["yes"],"severity":"Warning","hint":"Use concatMap","file":"/testproject/src/Hello113.hs","startLine":27,"startColumn":19,"endLine":27,"endColumn":45,"from":"concat . map f . baz . bar","to":"concatMap f . baz . bar","note":[],"refactorings":"[Replace {rtype = Expr, pos = SrcSpan {startLine = 27, startCol = 19, endLine = 27, endCol = 45}, subts = [(\"f\",SrcSpan {startLine = 27, startCol = 32, endLine = 27, endCol = 33}),(\"x\",SrcSpan {startLine = 27, startCol = 36, endLine = 27, endCol = 45})], orig = \"concatMap f . x\"}]"}]
      """

      When("converted to list of hlint infos")
      val hlintInfos = HLintComponent.parseHLintOutput(null, output)

      Then("it should contain right info")

      hlintInfos should have size 2

      val hlintInfo1 = hlintInfos.head
      hlintInfo1.file shouldEqual "/testproject/src/Hello113.hs"
      hlintInfo1.startLine shouldEqual 7
      hlintInfo1.note.size shouldEqual 1
      hlintInfo1.note.head shouldEqual "redundant parens"
      hlintInfo1.refactorings shouldEqual "[Replace {rtype = Expr, pos = SrcSpan {startLine = 7, startCol = 9, endLine = 7, endCol = 15}, subts = [(\"x\",SrcSpan {startLine = 7, startCol = 10, endLine = 7, endCol = 14})], orig = \"x\"}]"

      val hlintInfo2 = hlintInfos(1)
      hlintInfo2.file shouldEqual "/testproject/src/Hello113.hs"
      hlintInfo2.startLine shouldEqual 27
      hlintInfo2.endLine shouldEqual 27
      hlintInfo2.refactorings shouldEqual "[Replace {rtype = Expr, pos = SrcSpan {startLine = 27, startCol = 19, endLine = 27, endCol = 45}, subts = [(\"f\",SrcSpan {startLine = 27, startCol = 32, endLine = 27, endCol = 33}),(\"x\",SrcSpan {startLine = 27, startCol = 36, endLine = 27, endCol = 45})], orig = \"concatMap f . x\"}]"
    }
  }
}
