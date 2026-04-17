/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.highlighter

import com.intellij.codeInsight.daemon.impl.{DaemonCodeAnalyzerEx, HighlightInfo}
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.util.Processors

import java.util
import scala.collection.mutable
import scala.jdk.CollectionConverters.ListHasAsScala

object DaemonUtil {
  def getHighlights(project: Project,
                    document: Document,
                    minSeverity: HighlightSeverity,
                    startOffset: Int,
                    endOffset: Int
                   ): mutable.Buffer[HighlightInfo] = {
    val collection = new util.ArrayList[HighlightInfo]
    val collector = Processors.cancelableCollectProcessor(collection)
    DaemonCodeAnalyzerEx.processHighlights(document, project, minSeverity, startOffset, endOffset, collector)
    collection.asScala
  }

  def getHighlights(project: Project,
                    document: Document,
                    minSeverity: HighlightSeverity
                   ): mutable.Buffer[HighlightInfo] =
    getHighlights(project, document, minSeverity, 0, document.getTextLength)
}
