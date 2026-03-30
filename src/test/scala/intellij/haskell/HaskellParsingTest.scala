// SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
// SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
//
// SPDX-License-Identifier: Apache-2.0

package intellij.haskell

import com.intellij.testFramework.ParsingTestCase

class HaskellParsingTest extends ParsingTestCase("", "hs", new HaskellParserDefinition) {
  override def getTestDataPath: String = "src/test/testData/parsing-hs"

  def testPragma(): Unit = {
    doTest(true)
  }

  def testComplicatedPragma(): Unit = {
    doTest(true)
  }
}
