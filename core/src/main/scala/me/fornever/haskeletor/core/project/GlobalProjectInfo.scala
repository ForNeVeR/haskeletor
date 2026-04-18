/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.core.project

case class GlobalProjectInfo(ghcVersion: GhcVersion, ghcPath: String, ghcPkgPath: String, localDocRoot: String, snapshotDocRoot: String, packageDbPaths: PackageDbPaths, projectBinPaths: ProjectBinPaths, supportedLanguageExtensions: Iterable[String], availableStackagePackageNames: Iterable[String])

case class PackageDbPaths(globalPackageDbPath: String, snapshotPackageDbPath: String, localPackageDbPath: String)

case class ProjectBinPaths(compilerBinPath: String, localBinPath: String)
