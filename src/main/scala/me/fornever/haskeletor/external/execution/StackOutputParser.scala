/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.external.execution

import com.jetbrains.rd.util.reactive.{ISource, Signal}

/** Parses incremental Stack process output into structured events. */
class StackOutputParser {
  private val _signal = new Signal[StackOutputEvent]()
  private val buffer = new StringBuilder()

  /** Emits parsed events as soon as the parser recognizes a complete fragment. */
  val event: ISource[StackOutputEvent] = _signal

  /** Adds the next output chunk, which may contain partial lines or even a single character. */
  def addText(text: String): Unit = {
    text.foreach {
      case '\r' | '\n' | '\b' => flushBuffer()
      case character => buffer.append(character)
    }
  }

  /** Flushes the last buffered fragment after the process output has ended. */
  def finishProcess(): Unit = flushBuffer()

  private def flushBuffer(): Unit = {
    if (buffer.isEmpty) {
      return
    }

    val fragment = buffer.result()
    buffer.clear()

    if (!fragment.forall(_.isWhitespace)) {
      _signal.fire(StackOutputParser.parse(fragment))
    }
  }
}

private object StackOutputParser {
  private val ProgressPattern = raw"^Progress (\d+)/(\d+)(?:: (.*))?$$".r
  private val PackageStatusPattern = raw"^(\S(?:.*\S)?)\s+>\s+(.+)$$".r

  private def parse(fragment: String): StackOutputEvent = {
    fragment.trim match {
      case ProgressPattern(done, total, packages) =>
        Progress(done.toInt, total.toInt, Option(packages).map(parsePackages).getOrElse(IndexedSeq.empty))
      case PackageStatusPattern(packageName, status) => PackageStatus(packageName, status)
      case _ => TextOutput(fragment)
    }
  }

  private def parsePackages(packages: String): IndexedSeq[String] = {
    packages.split(",").iterator.map(_.trim).filter(_.nonEmpty).toIndexedSeq
  }
}

/** A structured event parsed from Stack process output. */
sealed trait StackOutputEvent

/** A plain text fragment that did not match a more specific Stack progress format. */
case class TextOutput(text: String) extends StackOutputEvent

/** A progress update that reports completed actions and currently active packages. */
case class Progress(done: Int, total: Int, packagesInProgress: IndexedSeq[String]) extends StackOutputEvent

/** A package status line such as `package-name > configure`. */
case class PackageStatus(packageName: String, status: String) extends StackOutputEvent
