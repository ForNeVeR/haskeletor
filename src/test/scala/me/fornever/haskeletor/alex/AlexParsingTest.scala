/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.alex

import com.intellij.testFramework.ParsingTestCase
import me.fornever.haskeletor.alex.lang.parser.AlexParserDefinition

class AlexParsingTest extends ParsingTestCase("", "x", new AlexParserDefinition):
  override def getTestDataPath: String = "src/test/testData/parsing"

  def testSimple(): Unit =
    doTest(true)

  def testRules(): Unit =
    doTest(true)

  def testRuleDescription(): Unit =
    doTest(true)

  def testLexerOwO(): Unit =
    doTest(true)

  def testMixedStatefulStateless(): Unit =
    doTest(true)
