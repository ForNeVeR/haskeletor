/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.runconfig.console;

import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class HaskellConsoleConfigurationForm extends SettingsEditor<HaskellConsoleConfiguration> {
    private final JPanel myPanel;
    private final JComboBox<String> targetComboBox;

    public HaskellConsoleConfigurationForm() {
        myPanel = new JPanel(new BorderLayout());
        myPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));

        final JPanel contentPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 1.0;
        constraints.insets = JBUI.emptyInsets();

        contentPanel.add(new JLabel("Stack target"), constraints);

        targetComboBox = new ComboBox<>();
        targetComboBox.setEnabled(true);
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = JBUI.insetsTop(2);
        contentPanel.add(targetComboBox, constraints);

        myPanel.add(contentPanel, BorderLayout.NORTH);
    }

    @Override
    protected void resetEditorFrom(@NotNull HaskellConsoleConfiguration config) {
        targetComboBox.removeAllItems();
        for (String name : config.getStackTargetNames()) {
            targetComboBox.addItem(name);
        }
        targetComboBox.setSelectedItem(config.getStackTarget());
    }

    @Override
    protected void applyEditorTo(@NotNull HaskellConsoleConfiguration config) {
        config.setStackTarget((String) targetComboBox.getSelectedItem());
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
