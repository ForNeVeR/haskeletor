/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.testIntegration

import com.intellij.openapi.project.Project
import com.intellij.psi.search.{FilenameIndex, GlobalSearchScope}
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.{PsiElement, PsiManager}
import com.intellij.testIntegration.TestFinder
import me.fornever.haskeletor.HaskellFile

import java.util
import scala.jdk.CollectionConverters._

/**
  * Triggered when the user uses the "Navigation / Test"  action (= jump to test shortcut)
  */
class HaskellTestFinder extends TestFinder {

  /**
    * Return the parent PsiFile of the PsiElement where the cursor was when the test finder was invoked, to handle some magic stuff, like the name displayed in "Choose Test for {file name}".
    */
  override def findSourceElement(psiElement: PsiElement): PsiElement = {
    PsiTreeUtil.getParentOfType(psiElement, classOf[HaskellFile])
  }

  private def findFilesInIndex(project: Project, name: String): Seq[PsiElement] = {
    FilenameIndex.getVirtualFilesByName(
      name,
      GlobalSearchScope.projectScope(project)
    ).asScala
      .toSeq
      .flatMap(vf => Option(PsiManager.getInstance(project).findFile(vf)))
      .map(_.asInstanceOf[PsiElement])
  }

  /**
    * Given a source PSI element, find all test files this element could be a source of.
    */
  override def findTestsForClass(psiElement: PsiElement): util.Collection[PsiElement] = {
    val testFileName = psiElement.getContainingFile.getName.replace(".hs", "Spec.hs")
    val testFiles = findFilesInIndex(psiElement.getProject, testFileName)
    testFiles.asJavaCollection
  }

  /**
    * Given a test PSI element, find all source files this element could be a test of.
    */
  override def findClassesForTest(psiElement: PsiElement): util.Collection[PsiElement] = {
    val sourceFileName = psiElement.getContainingFile.getName.replace("Spec.hs", ".hs")
    val sourceFiles = findFilesInIndex(psiElement.getProject, sourceFileName)
    sourceFiles.asJavaCollection
  }

  override def isTest(psiElement: PsiElement): Boolean = {
    psiElement.getContainingFile.getName.endsWith("Spec.hs")
  }
}
