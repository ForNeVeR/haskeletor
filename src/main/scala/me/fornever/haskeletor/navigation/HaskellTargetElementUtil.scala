/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.navigation

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.psi.{PsiElement, PsiReference}
import me.fornever.haskeletor.util.HaskellEditorUtil

import java.util
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._

class HaskellTargetElementUtil extends TargetElementUtil {

  override def getTargetCandidates(reference: PsiReference): util.Collection[PsiElement] = {
    reference match {
      case reference: HaskellReference =>
        val resolveResults = reference.multiResolve(false)
        val navigatableResults = ListBuffer[PsiElement]()

        for (r <- resolveResults) {
          r match {
            case NoResolveResult(noInfo) =>
              noInfo match {
                case NoInfoAvailable(_, _, _) => ()
                case ni => HaskellEditorUtil.showStatusBarMessage(reference.getElement.getProject, s"Navigation is not available at this moment: ${ni.message}")
              }
            case _ =>
              val element = r.getElement
              if (isNavigatableSource(element)) navigatableResults.append(element)
          }
        }
        navigatableResults.asJava
      case _ => super.getTargetCandidates(reference)
    }
  }
}
