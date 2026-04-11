# SPDX-FileCopyrightText: 2024-2026 Friedrich von Never <friedrich@fornever.me>
#
# SPDX-License-Identifier: Apache-2.0

param(
    [string] $RefName,
    [string] $RepositoryRoot = "$PSScriptRoot/.."
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

Write-Host "Determining version from ref `"$RefName`"…"
if ($RefName -match '^refs/tags/v') {
    $version = $RefName -replace '^refs/tags/v', ''
    Write-Host "Pushed ref is a version tag, version: $version"
} else {
    $propertiesFile = "$RepositoryRoot/gradle.properties"
    $content = Get-Content -Raw $propertiesFile
    $version = $null
    if ($content -match 'pluginVersion\s*=\s*(.*?)\n') {
        $version = $Matches[1]
    }

    if ($null -eq $version) {
        throw 'Cannot read version from Gradle metadata.'
    }

    Write-Host "Pushed ref is not a version tag, got version from the metadata: $version."
}

Write-Output $version
