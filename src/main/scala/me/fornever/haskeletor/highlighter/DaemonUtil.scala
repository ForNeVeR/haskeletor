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
  private def getHighlights(project: Project,
                            document: Document,
                            minSeverity: HighlightSeverity,
                            filter: Option[HighlightInfo => Boolean]
                           ): mutable.Buffer[HighlightInfo] = {
    val collection = new util.ArrayList[HighlightInfo]
    val collector = Processors.cancelableCollectProcessor(collection)
    val filteredCollector = filter match {
      case None => collector
      case Some(filter) => Processors.filter(collector, { info => filter(info) })
    }
    DaemonCodeAnalyzerEx.processHighlights(document, project, minSeverity, 0, document.getTextLength, filteredCollector)
    collection.asScala
  }

  def getDocumentHighlights(project: Project,
                            document: Document,
                            minSeverity: HighlightSeverity
                           ): mutable.Buffer[HighlightInfo] =
    getHighlights(project, document, minSeverity, None)

  def getHighlightsAtOffset(project: Project,
                            document: Document,
                            minSeverity: HighlightSeverity,
                            offset: Int
                           ): mutable.Buffer[HighlightInfo] = {
    // To get data about a particular offset, collect every highlighting and filter by "contains": replicates the
    // approach used in com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl.processHighlightsNearOffset.
    getHighlights(project, document, minSeverity, Some(info => info.contains(offset)))
  }
}
