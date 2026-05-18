/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "haskeletor"

include(
    "core",
    "projectModel",
    "settings",
    "spellchecker",
    "stack",
    "vcs"
)
