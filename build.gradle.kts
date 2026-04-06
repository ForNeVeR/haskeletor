/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.GenerateLexerTask
import org.jetbrains.intellij.platform.gradle.tasks.GenerateParserTask
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask

plugins {
    id("scala")
    alias(libs.plugins.gradle.jvm.wrapper)
    alias(libs.plugins.intellij.grammar.kit)
    alias(libs.plugins.intellij.platform)
}

jvmWrapper {
    linuxAarch64JvmUrl = "https://corretto.aws/downloads/latest/amazon-corretto-17-aarch64-linux-jdk.tar.gz"
    linuxX64JvmUrl = "https://corretto.aws/downloads/latest/amazon-corretto-17-x64-linux-jdk.tar.gz"
    macAarch64JvmUrl = "https://corretto.aws/downloads/latest/amazon-corretto-17-aarch64-macos-jdk.tar.gz"
    macX64JvmUrl = "https://corretto.aws/downloads/latest/amazon-corretto-17-x64-macos-jdk.tar.gz"
    windowsX64JvmUrl = "https://corretto.aws/downloads/latest/amazon-corretto-17-x64-windows-jdk.zip"
}

group = "me.fornever.haskeletor"
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation(libs.spray.json)
    implementation(libs.snakeyaml)
    implementation(libs.scaffeine)
    implementation(libs.directories)
    implementation(libs.fastparse)

    testImplementation(libs.scalatest)
    testImplementation(libs.scalatestplus.junit)

    intellijPlatform {
        intellijIdeaCommunity(providers.gradleProperty("platformVersion"))
        bundledPlugin("com.intellij.java")
        testFramework(TestFrameworkType.Platform)
    }
}

val generatedAlexLexerSourceBase = layout.buildDirectory.dir("generated/lexer/alex")
val generatedAlexParserSourceBase = layout.buildDirectory.dir("generated/parser/alex")
val generatedCabalSyntaxHighlightingLexerSourceBase = layout.buildDirectory.dir("generated/lexer/cabal-highlighting")
val generatedCabalParsingLexerSourceBase = layout.buildDirectory.dir("generated/lexer/cabal")
val generatedHaskellLexerSourceBase = layout.buildDirectory.dir("generated/lexer/haskell")
val generatedHaskellParserSourceBase = layout.buildDirectory.dir("generated/parser/haskell")

sourceSets {
    main {
        scala {
            srcDirs(
                "src/main/scala",
                generatedAlexLexerSourceBase,
                generatedAlexParserSourceBase,
                generatedCabalParsingLexerSourceBase,
                generatedCabalSyntaxHighlightingLexerSourceBase,
                generatedHaskellLexerSourceBase,
                generatedHaskellParserSourceBase
            )
        }
    }
    test {
        scala {
            srcDirs("src/test/scala")
        }
    }
}

intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")
    }
}

tasks {
    val generateAlexLexer by registering(GenerateLexerTask::class) {
        sourceFile = file("src/main/flex/_AlexLexer.flex")
        targetOutputDir = generatedAlexLexerSourceBase.map { it.dir("me/fornever/haskeletor/alex/lang/lexer") }
        purgeOldFiles = true
    }
    val generateCabalParsingLexer by registering(GenerateLexerTask::class) {
        sourceFile = file("src/main/flex/_CabalParsingLexer.flex")
        targetOutputDir = generatedCabalParsingLexerSourceBase.map { it.dir("me/fornever/haskeletor/cabal/lang/lexer") }
        purgeOldFiles = true
    }
    val generateCabalSyntaxHighlightingLexer by registering(GenerateLexerTask::class) {
        sourceFile = file("src/main/flex/_CabalSyntaxHighlightingLexer.flex")
        targetOutputDir = generatedCabalSyntaxHighlightingLexerSourceBase.map { it.dir("me/fornever/haskeletor/cabal/highlighting") }
        purgeOldFiles = true
    }
    val generateHaskellLexer by registering(GenerateLexerTask::class) {
        sourceFile = file("src/main/flex/_HaskellLexer.flex")
        targetOutputDir = generatedHaskellLexerSourceBase.map { it.dir("me/fornever/haskeletor") }
        purgeOldFiles = true
    }

    val generateAlexParser by registering(GenerateParserTask::class) {
        sourceFile = file("src/main/bnf/Alex.bnf")
        targetRootOutputDir = generatedAlexParserSourceBase
        purgeOldFiles = true
        pathToParser = "me/fornever/haskeletor/alex/lang/parser/AlexParser"
        pathToPsiRoot = "me/fornever/haskeletor/alex/lang/parser/psi"
    }
    val generateHaskellParser by registering(GenerateParserTask::class) {
        sourceFile = file("src/main/bnf/haskell.bnf")
        targetRootOutputDir = generatedHaskellParserSourceBase
        purgeOldFiles = true
        pathToParser = "me/fornever/haskeletor/HaskellParser"
        pathToPsiRoot = "me/fornever/haskeletor/psi"
    }

    withType<ScalaCompile> {
        dependsOn(
            generateAlexLexer,
            generateAlexParser,
            generateCabalParsingLexer,
            generateCabalSyntaxHighlightingLexer,
            generateHaskellLexer,
            generateHaskellParser
        )
        scalaCompileOptions.additionalParameters = listOf(
            "-deprecation", "-feature", "-unchecked"
        )
    }
    named<PrepareSandboxTask>("prepareSandbox") {
        from(listOf("README.md", "LICENSE.txt")) {
            into(pluginName)
        }
    }
    test {
        useJUnit()
        workingDir = project.projectDir
    }
}
