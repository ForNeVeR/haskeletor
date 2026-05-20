/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.testIntegration;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

/**
 * Binding of the "Create test" dialog. Responsible for fields declaration, validation and exiting the dialog (either by clicking OK, Cancel or closing the window).
 *
 * @implNote This is a Java file because Intellij integration of dialogs / forms only supports Java, not Scala.
 */
public class CreateHaskellTestDialog extends DialogWrapper {
    private final JPanel mainPanel;
    private final JTextField moduleName;

    public CreateHaskellTestDialog(@Nullable Project project) {
        // Basic configuration required by DialogWrapper
        super(project);
        moduleName = new JTextField();
        mainPanel = createMainPanel();
        init();
        setTitle("Create New Test Module");

        // Fields configuration
        moduleName.getDocument().addDocumentListener(new MandatoryTextFieldListener());
    }

    /**
     * @return A unique key so that dialog window resizing is remembered for future tests dialog creation
     */
    @Nullable
    @Override
    protected String getDimensionServiceKey() {
        return "#haskell.test.module.creation.dialog";
    }

    /**
     * Needed by Intellij to bind this class to the form.
     */
    @Override
    protected JComponent createCenterPanel() {
        return mainPanel;
    }

    private boolean isValid() {
        return !StringUtil.isEmptyOrSpaces(moduleName.getText());
    }

    public String getModuleName() {
        return moduleName.getText().trim();
    }

    public void setModuleName(String text) {
        moduleName.setText(text);
    }

    private JPanel createMainPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());

        final GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 0;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.fill = GridBagConstraints.NONE;
        labelConstraints.insets = JBUI.insetsRight(4);
        final JLabel label = new JLabel("Module name");
        label.setToolTipText("Name of the file (and module) that will be created");
        panel.add(label, labelConstraints);

        final GridBagConstraints textFieldConstraints = new GridBagConstraints();
        textFieldConstraints.gridx = 1;
        textFieldConstraints.gridy = 0;
        textFieldConstraints.weightx = 1.0;
        textFieldConstraints.anchor = GridBagConstraints.WEST;
        textFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        moduleName.setPreferredSize(new Dimension(150, moduleName.getPreferredSize().height));
        panel.add(moduleName, textFieldConstraints);

        return panel;
    }

    private class MandatoryTextFieldListener implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent documentEvent) {
            getOKAction().setEnabled(isValid());
        }

        @Override
        public void removeUpdate(DocumentEvent documentEvent) {
            getOKAction().setEnabled(isValid());
        }

        @Override
        public void changedUpdate(DocumentEvent documentEvent) {
            getOKAction().setEnabled(isValid());
        }
    }
}
