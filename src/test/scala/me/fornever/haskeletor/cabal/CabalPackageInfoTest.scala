/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.cabal

import com.intellij.testFramework.ParsingTestCase
import me.fornever.haskeletor.cabal.lang.parser.CabalParserDefinition

class CabalPackageInfoTest extends ParsingTestCase("", "cabal", new CabalParserDefinition) {
  override def getTestDataPath: String = "src/test/testData/parsing-hs"

  def testMainIsResolutionShouldNotCrashForTestSuiteStanza(): Unit = {
    val cabalFileText =
      """name: sample
        |version: 0.1.0.0
        |
        |test-suite sample-tests
        |  type: exitcode-stdio-1.0
        |  hs-source-dirs: test
        |  main-is: Main.hs
        |""".stripMargin
    val cabalPsiFile = createPsiFile("sample.cabal", cabalFileText).asInstanceOf[CabalFile]
    val packageInfo = new PackageInfo(cabalPsiFile, "C:\\tmp\\sample-project")

    val testSuites = packageInfo.testSuites.toSeq
    assert(testSuites.size == 1)
    assert(testSuites.head.mainIs.isEmpty)
  }
}
