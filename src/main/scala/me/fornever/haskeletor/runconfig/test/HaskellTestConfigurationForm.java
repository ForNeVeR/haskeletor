/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.runconfig.test;

import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.RawCommandLineEditor;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class HaskellTestConfigurationForm extends SettingsEditor<HaskellTestConfiguration> {
    private static final String HSPEC_OPTIONS_URL = "https://hspec.github.io/options.html";

    private final JPanel myPanel;
    private final RawCommandLineEditor stackArgsEditor;
    private final JComboBox<String> myTestsuiteComboBox;
    private final RawCommandLineEditor myTestFilterTextField;
    private final JButton button;

    public HaskellTestConfigurationForm() {
        stackArgsEditor = new RawCommandLineEditor();
        myTestsuiteComboBox = new ComboBox<>();
        myTestFilterTextField = new RawCommandLineEditor();
        button = new JButton();
        myPanel = createPanel();
        try {
            final URI uri = new URI(HSPEC_OPTIONS_URL);
            button.addActionListener(e -> open(uri));
            button.setText(HSPEC_OPTIONS_URL);
        } catch (URISyntaxException e) {
            Messages.showErrorDialog("Error while creating URI action to hspec site", "Can not create URI action");
        }
    }

    @Override
    protected void resetEditorFrom(@NotNull HaskellTestConfiguration config) {
        myTestsuiteComboBox.removeAllItems();
        for (String executable : config.getTestSuiteTargetNames()) {
            myTestsuiteComboBox.addItem(executable);
        }
        myTestsuiteComboBox.setSelectedItem(config.getTestSuiteTargetName());

        stackArgsEditor.setText(config.getStackArgs());
        myTestFilterTextField.setText(config.getTestArguments());
    }

    @Override
    protected void applyEditorTo(@NotNull HaskellTestConfiguration config) {
        config.setStackArgs(stackArgsEditor.getText());
        config.setTestSuiteTargetName((String) myTestsuiteComboBox.getSelectedItem());
        config.setTestArguments(myTestFilterTextField.getText());
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return myPanel;
    }

    @Override
    protected void disposeEditor() {
        myPanel.setVisible(false);
    }

    private JPanel createPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));

        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 1.0;
        constraints.insets = JBUI.emptyInsets();
        panel.add(new JLabel("Stack arguments:"), constraints);

        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = JBUI.emptyInsets();
        panel.add(stackArgsEditor, constraints);

        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Stack test arguments, see hspec website: "), constraints);

        constraints.gridx = 1;
        panel.add(button, constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(myTestFilterTextField, constraints);

        constraints.gridy = 4;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Testsuite:"), constraints);

        constraints.gridy = 5;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(myTestsuiteComboBox, constraints);

        constraints.gridy = 6;
        constraints.fill = GridBagConstraints.VERTICAL;
        constraints.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), constraints);

        return panel;
    }

    private static void open(URI uri) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(uri);
            } catch (IOException e) {
                Messages.showErrorDialog("Error while opening link", "Can not Open Link");
            }
        } else {
            Messages.showErrorDialog("Can not open link because Desktop is not supported", "Can not Open Link");
        }
    }
}
