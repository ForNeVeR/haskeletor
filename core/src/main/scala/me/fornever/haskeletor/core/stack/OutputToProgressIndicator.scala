/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.core.stack

import com.intellij.execution.process.{CapturingProcessAdapter, ProcessEvent, ProcessOutputType}
import com.intellij.ide.nls.NlsMessages
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.Key
import com.jetbrains.rd.util.lifetime.Lifetime
import me.fornever.haskeletor.core.HaskeletorBundle
import org.jetbrains.annotations.Nls

import scala.jdk.CollectionConverters.SeqHasAsJava

class OutputToProgressIndicator(@Nls title: String,
                                progressIndicator: ProgressIndicator)
  extends CapturingProcessAdapter() {

  private val parser = new StackOutputParser()
  parser.event.advise(Lifetime.Companion.getEternal, event => {
    event match {
      case TextOutput(_) => ()
      case PackageStatus(_, _) => ()
      case Progress(done, total, packagesInProgress) =>
        progressIndicator.setFraction(done.toDouble / total.toDouble)
        progressIndicator.setText(HaskeletorBundle.message("progress.installing-tool.text", title, done, total))
        progressIndicator.setText2(packagesInProgress match {
          case Seq() =>
            //noinspection ScalaExtractStringToBundle
            ""
          case _ =>
            val packagesToShow = packagesInProgress.take(3)
            val packagesToShowText = NlsMessages.formatNarrowAndList(packagesToShow.asJava)
            val otherPackageCount = packagesInProgress.size - packagesToShow.size
            HaskeletorBundle.message("progress.installing-tool.in-progress", packagesToShowText, otherPackageCount)
        })
    }

    kotlin.Unit.INSTANCE
  })

  override def onTextAvailable(event: ProcessEvent, outputType: Key[_]): Unit = {
    if (ProcessOutputType.isStderr(outputType)) {
      parser.addText(event.getText)
    }
  }

  override def processTerminated(event: ProcessEvent): Unit = parser.finishProcess()
}
