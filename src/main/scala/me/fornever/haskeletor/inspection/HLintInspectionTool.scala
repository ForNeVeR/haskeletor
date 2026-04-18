/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.inspection

import com.intellij.codeInspection._
import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.util.WaitFor
import external.HLintRefactoringsParser
import external.HLintRefactoringsParser._
import me.fornever.haskeletor.util._

import scala.concurrent.duration._
import scala.concurrent.{Future, blocking}
import scala.util.matching.Regex

class HLintInspectionTool extends LocalInspectionTool {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def checkFile(psiFile: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array[ProblemDescriptor] = {
    val project = psiFile.getProject
    HaskellNotificationGroup.logInfoEvent(project, s"HLint inspection is started for file ${psiFile.getName}")

    ProgressManager.checkCanceled()

    HaskellFileUtil.findDocument(psiFile) match {
      case Some(document) if HaskellProjectUtil.isSourceFile(psiFile) =>

        ProgressManager.checkCanceled()

        new WaitFor(500, 1) {
          override def condition(): Boolean = {
            ProgressManager.checkCanceled()
            !HaskellFileUtil.isDocumentUnsaved(document)
          }
        }

        ProgressManager.checkCanceled()

        val hlintInfos = ScalaFutureUtil.waitForValue(project,
          Future {
            blocking {
              HLintComponent.check(psiFile)
            }
          }, "Running HLint", timeout = 2.seconds) match {
          case Some(r) => r
          case None => Seq()
        }

        val problemsHolder = new ProblemsHolder(manager, psiFile, isOnTheFly)

        for {
          hlintInfo <- hlintInfos
          problemType = findProblemHighlightType(hlintInfo)
          if problemType != ProblemHighlightType.GENERIC_ERROR
          virtualFile <- HaskellFileUtil.findVirtualFile(psiFile)
        } yield {
          ProgressManager.checkCanceled()

          val quickFix = if (hlintInfo.refactorings == "[]") {
            HaskellNotificationGroup.logWarningEvent(project, s"No HLint refactorings for: ${hlintInfo.from} | ${hlintInfo.hint}")
            None
          } else {
            HLintRefactoringsParser.parseRefactoring(hlintInfo.refactorings) match {
              case Right(Delete(_, pos)) => createDeleteQuickfix(document, virtualFile, psiFile, pos, hlintInfo.hint)
              case Right(Replace(_, pos, subts, orig, deletes)) => createReplaceQuickfix(document, virtualFile, psiFile, pos, subts, orig, hlintInfo.hint, hlintInfo.note,
                deletes.flatMap(d => createDeleteQuickfix(document, virtualFile, psiFile, d.pos, hlintInfo.hint)))
              case Right(ModifyComment(pos, newComment)) => createModifyCommentQuickfix(document, virtualFile, psiFile, pos, newComment, hlintInfo.hint, hlintInfo.note)
              case Right(InsertComment(pos, insertComment)) => createInsertCommentQuickfix(document, virtualFile, psiFile, pos, insertComment, hlintInfo.hint, hlintInfo.note)
              case Right(RemoveAsKeyword(_)) => None // Fallback to manual applying suggestion
              case Left(error) =>
                HaskellNotificationGroup.logErrorEvent(project, error)
                None
            }
          }

          quickFix match {
            case Some(qf) =>
              val endElement = if (qf.getEndElement == null) qf.getStartElement else qf.getEndElement
              problemsHolder.registerProblem(new ProblemDescriptorBase(qf.getStartElement, endElement, hlintInfo.hint, Array(qf), problemType, false, null, true, isOnTheFly))
            case None => ManualHLintQuickfix.registerProblem(psiFile, virtualFile, hlintInfo, problemsHolder, problemType, isOnTheFly)
          }
        }

        HaskellNotificationGroup.logInfoEvent(project, s"HLint inspection is finished for file ${psiFile.getName}")

        if (hlintInfos.isEmpty) {
          null
        } else {
          problemsHolder.getResultsArray
        }
      case _ => null
    }
  }

  private def findOffsets(document: Document, virtualFile: VirtualFile, psiFile: PsiFile, pos: SrcSpan, newText: Option[String]) =
    for {
      (replaceStartOffset, replaceEndOffset, originalText) <- findTextWithOffsets(virtualFile, document, pos)
      startElement <- Option(psiFile.findElementAt(replaceStartOffset))
      endElement <- Option(psiFile.findElementAt(replaceEndOffset - 1))
    } yield {
      val replaceEndOffset2 = if (newText.exists(_.isEmpty) && replaceEndOffset < psiFile.getTextLength - 1) {
        replaceEndOffset + 1
      } else {
        replaceEndOffset
      }
      (replaceStartOffset, replaceEndOffset2, startElement, endElement, originalText)
    }

  private def createModifyCommentQuickfix(document: Document, virtualFile: VirtualFile, psiFile: PsiFile, pos: SrcSpan, newComment: String, hint: String, note: Seq[String]) = {
    for {
      (replaceStartOffset, replaceEndOffset, startElement, endElement, _) <- findOffsets(document, virtualFile, psiFile, pos, Some(newComment))
    } yield {
      new HLintReplaceQuickfix(document, virtualFile, startElement, endElement, replaceStartOffset, replaceEndOffset, newComment, hint, note, Seq())
    }
  }

  private def createInsertCommentQuickfix(document: Document, virtualFile: VirtualFile, psiFile: PsiFile, pos: SrcSpan, insertComment: String, hint: String, note: Seq[String]) = {
    for {
      (replaceStartOffset, replaceEndOffset, startElement, endElement, _) <- findOffsets(document, virtualFile, psiFile, pos, None)
    } yield {
      new HLintInsertCommentQuickfix(document, virtualFile, startElement, endElement, replaceStartOffset, replaceEndOffset, insertComment + "\n", hint, note)
    }
  }

  private def createReplaceQuickfix(document: Document, virtualFile: VirtualFile, psiFile: PsiFile, pos: SrcSpan, subts: Subts, orig: String, hint: String, note: Seq[String], deletes: Seq[HLintDeleteQuickfix]) = {
    for {
      (replaceStartOffset, replaceEndOffset, startElement, endElement, _) <- findOffsets(document, virtualFile, psiFile, pos, None)
      newText = subts.map({ case (x, pos) => (x, findText(virtualFile, document, pos)) }).collect {
        case (w, Some(toReplace)) => (w, toReplace)
      }.foldLeft(orig)({ case (x, y) => {
        val quotedRegex = Regex.quote(y._1)
        // replace not all occurrences of y._1, which is a subst key, but only those that are words.
        x.replaceAll(s"\\b${quotedRegex}\\b", y._2)
      } })
    } yield new HLintReplaceQuickfix(document, virtualFile, startElement, endElement, replaceStartOffset, replaceEndOffset, newText, hint, note, deletes)
  }

  private def createDeleteQuickfix(document: Document, virtualFile: VirtualFile, psiFile: PsiFile, pos: SrcSpan, hint: String) = {
    for {
      (replaceStartOffset, replaceEndOffset, startElement, endElement, originalText) <- findOffsets(document, virtualFile, psiFile, pos, Some(""))
    } yield new HLintDeleteQuickfix(document, virtualFile, startElement, endElement, replaceStartOffset, replaceEndOffset, hint, originalText)
  }

  private def findTextWithOffsets(virtualFile: VirtualFile, document: Document, pos: SrcSpan) = {
    for {
      startOffset <- LineColumnPosition.getOffset(virtualFile, LineColumnPosition(pos.startLine, pos.startCol))
      endOffset <- LineColumnPosition.getOffset(virtualFile, LineColumnPosition(pos.endLine, pos.endCol))
      text = document.getText(TextRange.create(startOffset, endOffset))
    } yield (startOffset, endOffset, text)
  }

  private def findText(virtualFile: VirtualFile, document: Document, pos: SrcSpan) =
    findTextWithOffsets(virtualFile, document, pos).map(_._3)

  private def findProblemHighlightType(hlintInfo: HLintInfo) = hlintInfo.severity match {
    case "Warning" => ProblemHighlightType.GENERIC_ERROR_OR_WARNING
    case "Error" => ProblemHighlightType.GENERIC_ERROR
    case _ => ProblemHighlightType.GENERIC_ERROR_OR_WARNING
  }
}
