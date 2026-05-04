/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.settings;

import com.intellij.openapi.components.*;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.APP)
@State(
        name = "HaskellConfiguration",
        storages = {
                @Storage(file = "haskeletor.xml")
        }
)
public final class HaskellSettingsPersistentStateComponent implements PersistentStateComponent<HaskellSettingsPersistentStateComponent.HaskellSettingsState> {

    private HaskellSettingsState haskellSettingsState = new HaskellSettingsState();

    @NotNull
    public static HaskellSettingsPersistentStateComponent getInstance() {
        final HaskellSettingsPersistentStateComponent haskellSettingsPersistentStateComponent = ServiceManager.getService(HaskellSettingsPersistentStateComponent.class);
        return haskellSettingsPersistentStateComponent != null ? haskellSettingsPersistentStateComponent : new HaskellSettingsPersistentStateComponent();
    }

    @NotNull
    public HaskellSettingsState getState() {
        return haskellSettingsState;
    }

    public void loadState(HaskellSettingsState haskellSettingsState) {
        this.haskellSettingsState = haskellSettingsState;
    }

    public static class HaskellSettingsState {
        public Integer replTimeout = 30;
        public String hlintOptions = "";
        public Boolean useSystemGhc = false;
        public Boolean reformatCodeBeforeCommit = false;
        public Boolean optimizeImportsBeforeCommit = false;
        public String newProjectTemplateName = "new-template";
        public String cachePath = GlobalInfo.DefaultCachePath();
        public String hlintPath = "";
        public String hooglePath = "";
        public String ormoluPath = "";
        public String stylishHaskellPath = "";
        public String stackPath = "";
        public Boolean customTools = false;
        public String extraStackArguments = "";
        public String defaultGhcOptions = "-Wall -Wcompat -Wincomplete-record-updates -Wincomplete-uni-patterns -Wredundant-constraints";
    }
}
