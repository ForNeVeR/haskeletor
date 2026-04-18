/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.stack

import com.intellij.build.BuildView
import com.intellij.build.FilePosition
import com.intellij.build.events.FileMessageEvent
import com.intellij.build.events.MessageEvent
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsSafe
import me.fornever.haskeletor.core.compiler.CompilationProblem
import me.fornever.haskeletor.core.compiler.HaskellCompilationResultHelper
import scala.Some
import java.util.concurrent.LinkedBlockingDeque

class BuildViewProcessAdapter(private val buildView: BuildView, private val buildId: Int) : ProcessListener {

    private val ansiEscapeDecoder = AnsiEscapeDecoder()
    private val previousMessageLines = LinkedBlockingDeque<String>()

    private var globalError = false

    private fun decodeAnsiCommandsToString(ansi: String, outputType: Key<*>): String {
        val buffer = StringBuilder()
        ansiEscapeDecoder.escapeText(ansi, outputType, { text, _ -> buffer.append(text) })
        return buffer.toString()
    }

    override fun onTextAvailable(
        event: ProcessEvent,
        outputType: Key<*>
    ) {
        val text = decodeAnsiCommandsToString(event.text, outputType)
        addToBuildView(text, outputType)
    }

    override fun processTerminated(event: ProcessEvent) {
        addLastMessageToBuildView()
    }

    private fun addToBuildView(text: String, outputType: Key<*>) {
        val text = text.trim()
        if (text.isNotEmpty()) {
            if (ProcessOutputType.isStderr(outputType)) {
                if (text.startsWith("Error:") && text.endsWith(":")
                    || text.startsWith("Unable to parse")
                    || text.startsWith("Error parsing")) {
                    globalError = true // To get also all lines after this line indicated as error AND in order
                }

                // End of sentence which was over multiple lines
                if (!previousMessageLines.isEmpty() && !text.startsWith("  ")) {
                    addMessage()
                }

                previousMessageLines.add(text)
            } else if (text.startsWith("Warning:")) {
                onGlobalWarning(text)
            } else if (text.startsWith("Error:")) {
                onGlobalError(text)
            } else {
                onGlobalInfo(text)
            }
        }
    }

    private fun addLastMessageToBuildView() {
        if (!previousMessageLines.isEmpty()) {
            addMessage()
        }
    }

    private fun addMessage() {
        val errorMessageLine = previousMessageLines.joinToString(" ")
        when(val problem = HaskellCompilationResultHelper.parseErrorLine(errorMessageLine.replace("\n", " "))) {
            is Some if problem.get().isWarning() -> onFileWarning(problem.get())
            is Some -> onFileError(problem.get())
            else -> {
                if (globalError || errorMessageLine.contains("ExitFailure") || errorMessageLine.startsWith("Error:")) {
                    onGlobalError(errorMessageLine)
                } else if (errorMessageLine.startsWith("Warning:")) {
                    onGlobalWarning(errorMessageLine)
                } else {
                    onGlobalInfo(errorMessageLine)
                }
            }
        }

        previousMessageLines.clear()
    }

    private fun onGlobalError(text: @NlsSafe String) {
        buildView.onEvent(
            buildId,
            MessageEvent.builder(
                text,
                MessageEvent.Kind.ERROR
            ).build()
        )
    }

    private fun onGlobalInfo(text: @NlsSafe String) {
        buildView.onEvent(
            buildId,
            MessageEvent.builder(
                text,
                MessageEvent.Kind.INFO
            ).build()
        )
    }

    private fun onGlobalWarning(text: @NlsSafe String) {
        buildView.onEvent(
            buildId,
            MessageEvent.builder(
                text,
                MessageEvent.Kind.WARNING
            ).build()
        )
    }

    private fun CompilationProblem.toFileMessageEvent(kind: MessageEvent.Kind) =
        FileMessageEvent.builder(
            message(),
            kind,
            FilePosition(filePath().toFile(), columnNr(), lineNr())
        ).build()

    private fun onFileWarning(problem: CompilationProblem) {
        buildView.onEvent(
            buildId,
            problem.toFileMessageEvent(MessageEvent.Kind.WARNING)
        )
    }

    private fun onFileError(problem: CompilationProblem) {
        buildView.onEvent(
            buildId,
            problem.toFileMessageEvent(MessageEvent.Kind.ERROR)
        )
    }
}
