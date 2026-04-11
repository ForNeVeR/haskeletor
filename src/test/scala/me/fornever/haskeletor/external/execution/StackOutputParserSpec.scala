/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.external.execution

import com.jetbrains.rd.util.lifetime.Lifetime
import org.junit.runner.RunWith
import org.scalatest.GivenWhenThen
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.collection.mutable.ArrayBuffer

@RunWith(classOf[JUnitRunner])
class StackOutputParserSpec extends AnyFunSpec with Matchers with GivenWhenThen {

  describe("StackOutputParser") {
    it("should produce the same event sequence for all input chunking modes") {
      Given("the recorded Stack stderr output")
      //noinspection UseOptimizedEelFunctions
      val text = Files.readString(
        Path.of("src", "test", "testData", "progress", "install-hlint-stderr.txt"),
        StandardCharsets.UTF_8
      )

      When("the parser receives the fixture as a whole, by lines, and character by character")
      val wholeTextEvents = parseOutput(Seq(text))
      val lineByLineEvents = parseOutput(text.split("(?<=\\n)", -1).toSeq.filter(_.nonEmpty))
      val characterByCharacterEvents = parseOutput(text.iterator.map(_.toString).toSeq)

      Then("every mode should produce the same event sequence")
      lineByLineEvents shouldEqual wholeTextEvents
      characterByCharacterEvents shouldEqual wholeTextEvents

      And("representative fragments should be parsed into the expected event types")
      wholeTextEvents(8) shouldEqual Progress(0, 67, IndexedSeq.empty)
      wholeTextEvents(9) shouldEqual PackageStatus("StateVar", "configure")
      wholeTextEvents should contain(Progress(
        2,
        67,
        IndexedSeq(
          "base-compat",
          "base-orphans",
          "clock",
          "cmdargs",
          "colour",
          "contravariant",
          "data-default-instances-containers",
          "dlist",
          "file-embed"
        )
      ))
      wholeTextEvents should contain(PackageStatus("hlint", "copy/register"))
      wholeTextEvents should contain(TextOutput("Completed 67 action(s)."))
    }
  }

  private def parseOutput(chunks: Seq[String]): Seq[StackOutputEvent] = {
    val parser = new StackOutputParser
    val events = ArrayBuffer.empty[StackOutputEvent]

    parser.event.advise(Lifetime.Companion.getEternal, { event: StackOutputEvent =>
      events += event
      kotlin.Unit.INSTANCE
    })
    chunks.foreach(parser.addText)
    parser.finishProcess()

    events.toSeq
  }
}
