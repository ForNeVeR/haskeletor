/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.action

import com.intellij.ide.actions.CreateFileFromTemplateDialog.Builder
import com.intellij.ide.actions.{CreateFileAction, CreateFileFromTemplateAction}
import com.intellij.ide.fileTemplates.{FileTemplate, FileTemplateManager, FileTemplateUtil}
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.{DumbAware, Project}
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.{InputValidatorEx, Messages}
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.{PsiDirectory, PsiFile}
import me.fornever.haskeletor.icons.HaskellIcons
import me.fornever.haskeletor.util.HaskellFileUtil
import me.fornever.haskeletor.util.index.HaskellModuleNameIndex

import java.text.ParseException
import java.util.Properties

object CreateHaskellFileAction:
  private final val HaskellModule = "Haskell Module"

class CreateHaskellFileAction extends CreateFileFromTemplateAction(CreateHaskellFileAction.HaskellModule, "", HaskellIcons.HaskellFileLogo) with DumbAware:

  override def buildDialog(project: Project, directory: PsiDirectory, builder: Builder): Unit =
    builder.setTitle(CreateHaskellFileAction.HaskellModule).addKind("Empty module", HaskellIcons.HaskellFileLogo, "Haskell Module").setValidator(new InputValidatorEx {

      override def checkInput(inputString: String): Boolean = {
        true
      }

      override def canClose(inputString: String): Boolean = {
        !StringUtil.isEmptyOrSpaces(inputString) && getErrorText(inputString) == null
      }

      def getErrorText(inputString: String): String = {
        val checkString = if inputString.contains(".") then {
          inputString.trim.split("\\.").last
        } else {
          inputString
        }

        if !StringUtil.isCapitalized(checkString) then {
          s"'$inputString' is not a valid Haskell module name"
        } else {
          null
        }
      }
    })

  override def createFileFromTemplate(fileName: String, template: FileTemplate, fileDir: PsiDirectory): PsiFile =
    createFileFromTemplate(fileName, template, fileDir, new Properties())

  def createFileFromTemplate(fileName: String, template: FileTemplate, fileDir: PsiDirectory, additionalProps: Properties): PsiFile =
    val path = HaskellFileUtil.getAbsolutePath(fileDir.getVirtualFile)
    val pathItems = ProjectRootManager.getInstance(fileDir.getProject)
      .getContentSourceRoots
      .map(HaskellFileUtil.getAbsolutePath)
      .find(path.startsWith)
      .map(s => if s != path then {
        // See https://github.com/rikvdkleij/intellij-haskell/issues/339
        path.replace(s + "/", "").split("/").toList
      } else {
        List()
      })

    val createFileResult = if fileName.contains(".") then
      var targetDir = fileDir
      val names = fileName.trim().split("\\.").toList
      val moduleName = names.last
      val prefixes = names.dropRight(1)

      prefixes.foreach(dirName => {
        targetDir = Option(targetDir.findSubdirectory(dirName)).getOrElse(targetDir.createSubdirectory(dirName))
      })

      createFileFromTemplate(pathItems.map(_ ++ prefixes), moduleName, template, targetDir, additionalProps)
    else
      createFileFromTemplate(pathItems, fileName, template, fileDir, additionalProps)

    createFileResult match
      case None => null
      case Some((psiFile, moduleName)) =>
        HaskellModuleNameIndex.invalidateModuleName(psiFile.getProject, moduleName)
        psiFile

  private def createFileFromTemplate(pathItems: Option[List[String]], fileName: String, template: FileTemplate, fileDir: PsiDirectory, additionalProps: Properties): Option[(PsiFile, String)] =
    pathItems match
      case None => None
      case Some(items) =>
        // Adapted from super definition.
        val mkdirs = new CreateFileAction.MkDirs(fileName, fileDir)
        val name = mkdirs.newName
        val dir = mkdirs.directory
        val project = dir.getProject

        val nameWithmodulePrefix = if invalidPathItems(items) || items.isEmpty then
          name
        else
          items.mkString(".") + "." + name

        // Patch props with custom property.
        val props = FileTemplateManager.getInstance(project).getDefaultProperties()
        props.setProperty("NAME", nameWithmodulePrefix)
        additionalProps.forEach((k, v) => props.put(k, v)) // putAll does not work because of scalac bug

        val element = FileTemplateUtil.createFromTemplate(template, name, props, dir)
        val psiFile = element.getContainingFile.getOriginalFile

        try
          val virtualFile = HaskellFileUtil.findVirtualFile(psiFile)
          virtualFile.foreach(vFile => {
            FileEditorManager.getInstance(project).openFile(vFile, true)
            Option(getDefaultTemplateProperty).foreach(defaultTemplateProperty => {
              PropertiesComponent.getInstance(project).setValue(defaultTemplateProperty, template.getName)
            })
          })
        catch
          case e: ParseException => Messages.showErrorDialog(project, "Error parsing Haskell Module template: " + e.getMessage, "Create File from Template");

        Some(psiFile, nameWithmodulePrefix)

  /**
   * Returns true if any directory name starts with a lower case letter.
   */
  private def invalidPathItems(pathItems: List[String]): Boolean =
    pathItems.exists(s => s.isEmpty || !StringUtil.isCapitalized(s.substring(0, 1)))

  protected def getActionName(directory: PsiDirectory, newName: String, templateName: String): String =
    CreateHaskellFileAction.HaskellModule

