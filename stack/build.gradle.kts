/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    alias(libs.plugins.kotlin.jvm)
    id("org.jetbrains.intellij.platform.module")
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea(libs.versions.intellij.platform)

        bundledPlugin("org.jetbrains.plugins.terminal")

        testFramework(TestFrameworkType.JUnit5)
        testFramework(TestFrameworkType.Platform)
    }

    implementation(project(":core"))
    implementation(project(":settings"))

    testImplementation(libs.junit.jupiter.api)

    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit4)
}

tasks {
    test {
        useJUnitPlatform()
    }
}

