/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.runconfig.console

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

import java.util.concurrent.ConcurrentHashMap
import scala.collection.concurrent
import scala.jdk.CollectionConverters.*

object HaskellConsoleViewMap:
  private val consoleViews = new ConcurrentHashMap[Editor, HaskellConsoleView]().asScala

  def addConsole(console: HaskellConsoleView): Unit =
    consoleViews.put(console.getConsoleEditor, console)

  def delConsole(console: HaskellConsoleView): Unit =
    consoleViews.remove(console.getConsoleEditor)

  def getConsole(editor: Editor): Option[HaskellConsoleView] =
    consoleViews.get(editor)

  def getConsole(editor: Project): Option[HaskellConsoleView] =
    consoleViews.values.find(console => console.project == editor && console.isShowing)

  // File is project file and not file which represents console
  val projectFileByConfigName: concurrent.Map[String, PsiFile] = new ConcurrentHashMap[String, PsiFile]().asScala
