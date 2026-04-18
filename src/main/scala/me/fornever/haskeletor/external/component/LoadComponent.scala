/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.external.component

import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiFile
import me.fornever.haskeletor.annotator.HaskellAnnotator
import me.fornever.haskeletor.external.repl.ProjectStackRepl.Loaded
import me.fornever.haskeletor.external.repl._
import me.fornever.haskeletor.psi.HaskellPsiUtil
import me.fornever.haskeletor.util.ScalaUtil
import me.fornever.haskeletor.util.index.HaskellModuleNameIndex

private[component] object LoadComponent {

  def isModuleLoaded(moduleName: Option[String], psiFile: PsiFile): Boolean = {
    isFileLoaded(psiFile) || {
      for {
        mn <- moduleName
        repl <- StackReplsManager.getProjectRepl(psiFile)
      } yield repl.isModuleLoaded(mn)
    }.contains(true)
  }

  def load(psiFile: PsiFile, fileModified: Boolean): Option[CompilationResult] = {
    val project = psiFile.getProject

    StackReplsManager.getProjectRepl(psiFile).flatMap(projectRepl => {

      // The REPL is not started if target which it's depends on has compile errors at the moment of start.
      synchronized {
        if (!projectRepl.available && !projectRepl.starting) {
          projectRepl.start()
        }
      }

      ProjectLibraryBuilder.checkLibraryBuild(project, projectRepl.projectReplTargets)
      val moduleName = HaskellPsiUtil.findModuleName(psiFile)

      {
        if (HaskellAnnotator.getNotLoadedFiles(project).contains(psiFile)) {
          HaskellAnnotator.removeNotLoadedFile(psiFile)
          projectRepl.load(psiFile, fileModified, moduleName, forceNoReload = true)
        } else {
          projectRepl.load(psiFile, fileModified, moduleName)
        }
      } match {
        case Some((loadOutput, loadFailed)) =>
          ApplicationManager.getApplication.executeOnPooledThread(ScalaUtil.runnable {

            DefinitionLocationComponent.invalidate(project)
            HaskellModuleNameIndex.invalidateNotFoundEntries(project)
            TypeInfoComponent.invalidateAll(project)
            NameInfoComponent.invalidateProjectInfo(project)

            if (!loadFailed) {
              moduleName.foreach(mn => {
                NameInfoComponent.invalidateNotFound(project)
                DefinitionLocationComponent.invalidateNotFound(project)
                BrowseModuleComponent.invalidateModuleNames(project, Seq(mn))
                FileModuleIdentifiers.invalidate(mn)
              })

            }
            if (!project.isDisposed) {
              DocumentationManager.getInstance(project).updateToolwindowContext()
            }
          })
          Some(HaskellCompilationResultHelper.createCompilationResult(psiFile, loadOutput.stderrLines, loadFailed))
        case _ => None
      }
    })
  }

  private def isFileLoaded(psiFile: PsiFile): Boolean = {
    val projectRepl = StackReplsManager.getProjectRepl(psiFile)
    projectRepl.map(_.isFileLoaded(psiFile)).contains(Loaded)
  }
}
