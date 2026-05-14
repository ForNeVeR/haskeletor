/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.editor;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

@State(name = "HaskellFoldingSettings", storages = {
        @Storage("editor.xml"),
        @Storage(value = "editor.codeinsight.xml", deprecated = true),
}, reportStatistic = true)
public class HaskellFoldingSettings implements PersistentStateComponent<HaskellFoldingSettings.State> {
    private final HaskellFoldingSettings.State myState = new State();

    public static HaskellFoldingSettings getInstance() {
        return ApplicationManager.getApplication().getService(HaskellFoldingSettings.class);
    }

    public boolean isCollapseImports() {
        return myState.COLLAPSE_IMPORTS;
    }

    public boolean isCollapseFileHeader() {
        return myState.COLLAPSE_FILE_HEADER;
    }

    public boolean isCollapseTopLevelExpression() {
        return myState.COLLAPSE_TOP_LEVEL_EXPRESSION;
    }

    @Override
    @NotNull
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        XmlSerializerUtil.copyBean(state, myState);
    }

    public static final class State {
        public boolean COLLAPSE_IMPORTS = false;
        public boolean COLLAPSE_FILE_HEADER = false;
        public boolean COLLAPSE_TOP_LEVEL_EXPRESSION = false;
    }
}
