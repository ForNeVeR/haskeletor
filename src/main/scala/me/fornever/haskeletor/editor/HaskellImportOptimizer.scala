/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.editor

import com.intellij.lang.ImportOptimizer
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.TokenType.WHITE_SPACE
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.{PsiElement, PsiFile}
import me.fornever.haskeletor.HaskellFile
import me.fornever.haskeletor.highlighter.DaemonUtil
import me.fornever.haskeletor.psi.HaskellPsiExtensions._
import me.fornever.haskeletor.psi.HaskellPsiUtil
import me.fornever.haskeletor.psi.HaskellTypes._
import me.fornever.haskeletor.util.{LineColumnPosition, ScalaUtil}

import scala.jdk.CollectionConverters._
import scala.util.matching.Regex

class HaskellImportOptimizer extends ImportOptimizer {

  override def supports(psiFile: PsiFile): Boolean = psiFile.isInstanceOf[HaskellFile] && HaskellProjectUtil.isSourceFile(psiFile)

  override def processFile(psiFile: PsiFile): Runnable = {
    () => HaskellImportOptimizer.removeRedundantImports(psiFile)
  }
}

object HaskellImportOptimizer {
  final val WarningRedundantImport: Regex = """.*The (?:qualified )?import of [`|‘]([^'’]+)['|’] is redundant.*""".r
  final val WarningRedundant2Import: Regex = """.*The (?:qualified )?import of [`|‘]([^'’]+)['|’] from module [`|‘]([^'’]+)['|’] is redundant.*""".r

  def removeRedundantImports(psiFile: PsiFile): Boolean = {
    val document = HaskellFileUtil.findDocument(psiFile)
    val warnings = document
      .map(d => DaemonUtil.getDocumentHighlights(psiFile.getProject, d, HighlightSeverity.WARNING).toSeq)
      .getOrElse(Seq.empty)

    warnings.foreach(w => w.getDescription match {
      case HaskellImportOptimizer.WarningRedundantImport(mn) => removeRedundantImport(psiFile, mn, getLineNr(psiFile, w.getStartOffset))
      case HaskellImportOptimizer.WarningRedundant2Import(idNames, mn) => removeRedundantImportIds(psiFile, mn, idNames.split(',').toSeq.map(_.trim), getLineNr(psiFile, w.getStartOffset))
      case _ => ()
    })
    true
  }

  private def getLineNr(psiFile: PsiFile, element: PsiElement) = {
    val offset = element.getTextRange.getStartOffset
    LineColumnPosition.fromOffset(psiFile.getVirtualFile, offset).map(_.lineNr)
  }

  private def getLineNr(psiFile: PsiFile, offset: Int) = {
    LineColumnPosition.fromOffset(psiFile.getVirtualFile, offset).map(_.lineNr)
  }

  def removeRedundantImport(psiFile: PsiFile, moduleName: String, lineNr: Option[Int]): Unit = {
    HaskellPsiUtil.findImportDeclarations(psiFile).find(d => d.getModuleName.contains(moduleName) && getLineNr(psiFile, d) == lineNr).foreach { importDeclaration =>

      val spaces = Option(PsiTreeUtil.findSiblingForward(importDeclaration, WHITE_SPACE, true, null))
      val newline = spaces.flatMap(s => Option(PsiTreeUtil.findSiblingForward(s, HS_NEWLINE, true, null)))
      WriteCommandAction.runWriteCommandAction(psiFile.getProject, ScalaUtil.computable {
        spaces.foreach(_.delete())
        newline.foreach(_.delete())
        importDeclaration.delete()
      })
    }
  }

  import me.fornever.haskeletor.psi.HaskellTypes._

  def removeRedundantImportIds(psiFile: PsiFile, moduleName: String, idNames: Seq[String], lineNr: Option[Int]): Unit = {
    HaskellPsiUtil.findImportDeclarations(psiFile).find(d => d.getModuleName.contains(moduleName) && getLineNr(psiFile, d) == lineNr).foreach { importDeclaration =>
      val prefix = Option(importDeclaration.getImportQualifiedAs).map(_.getQualifier.getName).orElse(importDeclaration.getModuleName)
      val idsToRemove = importDeclaration.getImportSpec.getImportIdsSpec.getImportIdList.asScala.filter(qn => idNames.exists(idn => idn == qn.getText || prefix.exists(p => idn == p + "." + qn.getText)))
      idsToRemove.foreach { iid =>
        val commaToRemove = Option(PsiTreeUtil.findSiblingForward(iid, HS_COMMA, true, null)).orElse(Option(PsiTreeUtil.findSiblingBackward(iid, HS_COMMA, true, null)))
        val whiteSpaceRemove = Option(PsiTreeUtil.findSiblingBackward(iid, WHITE_SPACE, true, null)).orElse(Option(PsiTreeUtil.findSiblingForward(iid, WHITE_SPACE, true, null)))
        val newline = whiteSpaceRemove.flatMap(s => Option(PsiTreeUtil.findSiblingForward(s, HS_NEWLINE, true, null)))
        WriteCommandAction.runWriteCommandAction(psiFile.getProject, ScalaUtil.computable {
          whiteSpaceRemove.foreach(_.delete())
          newline.foreach(_.delete())
          commaToRemove.foreach(_.delete())
          iid.delete()
        })
      }
    }
  }
}
