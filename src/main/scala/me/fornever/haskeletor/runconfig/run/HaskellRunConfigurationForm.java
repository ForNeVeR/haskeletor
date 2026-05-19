/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.runconfig.run;

import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.RawCommandLineEditor;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class HaskellRunConfigurationForm extends SettingsEditor<HaskellRunConfiguration> {
    private final JPanel myPanel;
    private final RawCommandLineEditor stackArgsEditor;
    private final JComboBox<String> myExecutableComboBox;
    private final RawCommandLineEditor programArgsEditor;

    public HaskellRunConfigurationForm() {
        myPanel = new JPanel(new BorderLayout());
        myPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));

        final JPanel contentPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = JBUI.emptyInsets();

        contentPanel.add(new JLabel("Stack arguments:"), constraints);

        stackArgsEditor = new RawCommandLineEditor();
        stackArgsEditor.setText("");
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = JBUI.insetsTop(2);
        contentPanel.add(stackArgsEditor, constraints);

        constraints.gridy = 2;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = JBUI.insetsTop(6);
        contentPanel.add(new JLabel("Executable:"), constraints);

        myExecutableComboBox = new ComboBox<>();
        constraints.gridy = 3;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = JBUI.insetsTop(2);
        contentPanel.add(myExecutableComboBox, constraints);

        constraints.gridy = 4;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = JBUI.insetsTop(6);
        contentPanel.add(new JLabel("Program arguments:"), constraints);

        programArgsEditor = new RawCommandLineEditor();
        programArgsEditor.setText("");
        constraints.gridy = 5;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = JBUI.insetsTop(2);
        contentPanel.add(programArgsEditor, constraints);

        myPanel.add(contentPanel, BorderLayout.NORTH);
    }

    @Override
    protected void resetEditorFrom(@NotNull HaskellRunConfiguration config) {
        myExecutableComboBox.removeAllItems();
        for (String executable : config.getExecutableNames()) {
            myExecutableComboBox.addItem(executable);
        }
        myExecutableComboBox.setSelectedItem(config.getExecutableName());

        stackArgsEditor.setText(config.getStackArgs());
        programArgsEditor.setText(config.getProgramArgs());
    }

    @Override
    protected void applyEditorTo(@NotNull HaskellRunConfiguration config) {
        config.setStackArgs(stackArgsEditor.getText());
        config.setExecutableName((String) myExecutableComboBox.getSelectedItem());
        config.setProgramArgs(programArgsEditor.getText());
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
}
