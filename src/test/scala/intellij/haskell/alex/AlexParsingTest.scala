// SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
// SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
//
// SPDX-License-Identifier: Apache-2.0

package intellij.haskell.alex

import com.intellij.testFramework.ParsingTestCase
import intellij.haskell.alex.lang.parser.AlexParserDefinition

class AlexParsingTest extends ParsingTestCase("", "x", new AlexParserDefinition) {
  override def getTestDataPath: String = "src/test/testData/parsing"

  def testSimple(): Unit = {
    doTest(true)
  }

  def testRules(): Unit = {
    doTest(true)
  }

  def testRuleDescription(): Unit = {
    doTest(true)
  }

  def testLexerOwO(): Unit = {
    doTest(true)
  }

  def testMixedStatefulStateless(): Unit = {
    doTest(true)
  }
}
