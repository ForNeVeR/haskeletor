/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.ui

import com.intellij.openapi.ui.DialogWrapper

import java.awt.BorderLayout
import javax.swing.{JComponent, JLabel, JPanel, JTextField}

class EnterNameDialog(prompt: String, suggestion: String = "") extends DialogWrapper(true):
  private val textField = if suggestion.isEmpty then new JTextField(10) else new JTextField(suggestion)
  init()
  setTitle(prompt)
  override def createCenterPanel(): JComponent =
    val dialogPanel: JPanel = new JPanel(new BorderLayout)

    val label: JLabel = new JLabel(prompt)
    dialogPanel.add(label, BorderLayout.NORTH)

    dialogPanel.add(textField, BorderLayout.SOUTH)

    dialogPanel

  override def getPreferredFocusedComponent: JComponent = textField

  def getName: String = textField.getText

