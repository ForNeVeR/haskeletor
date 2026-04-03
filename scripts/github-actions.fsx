let licenseHeader = """
# SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
# SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
# SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
#
# SPDX-License-Identifier: Apache-2.0

# This file is auto-generated.""".Trim()

#r "nuget: Generaptor.Library, 1.11.0"

open Generaptor
open Generaptor.GitHubActions

open type Generaptor.GitHubActions.Commands

let workflows = [
    let linux = "ubuntu-24.04"
    let runners = [linux]

    let workflow name body = workflow name [
        header licenseHeader
        yield! body
    ]

    workflow "main" [
        name "Gradle CI"
        onPushTo "master"
        onPushTo "renovate/**"
        onPullRequestTo "master"
        job "build" [
            yield! runners |> Seq.map runsOn
            step(
                usesSpec = Auto "actions/checkout"
            )
            step(
                name = "Cache downloaded JDK",
                usesSpec = Auto "actions/cache",
                options = Map.ofList [
                    "path", [
                        "~/.local/share/gradle-jvm"
                        "~/AppData/Local/gradle-jvm"
                    ] |> String.concat "\n"
                    "key", "${{ runner.os }}.jvm.${{ hashFiles('gradlew*') }}"
                ]
            )
            step(
                name = "Setup Gradle",
                usesSpec = Auto "gradle/actions/setup-gradle"
            )
            step(
                name = "Build plugin",
                run = "./gradlew buildPlugin"
            )
            step(
                name = "Run tests",
                run = "./gradlew test"
            )
        ]
        job "encoding" [
            runsOn linux
            step(
                name = "Check out the sources",
                usesSpec = Auto "actions/checkout"
            )
            step(
                name = "Verify encoding",
                shell = "pwsh",
                run = "Install-Module VerifyEncoding -Repository PSGallery -RequiredVersion 2.3.0 -Force && Test-Encoding"
            )
        ]
        job "licenses" [
            runsOn linux
            step(
                name = "Check out the sources",
                usesSpec = Auto "actions/checkout"
            )
            step(
                name = "REUSE license check",
                usesSpec = Auto "fsfe/reuse-action"
            )
        ]
        job "verify-workflows" [
            runsOn linux

            setEnv "DOTNET_CLI_TELEMETRY_OPTOUT" "1"
            setEnv "DOTNET_NOLOGO" "1"
            setEnv "NUGET_PACKAGES" "${{ github.workspace }}/.github/nuget-packages"
            step(
                usesSpec = Auto "actions/checkout"
            )
            step(
                usesSpec = Auto "actions/setup-dotnet"
            )
            step(
                run = "dotnet fsi ./scripts/github-actions.fsx verify"
            )
        ]
    ]
]
exit <| EntryPoint.Process fsi.CommandLineArgs workflows
