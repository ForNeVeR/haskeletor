/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("scalaModuleBase")
    id("org.jetbrains.intellij.platform.module")
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

scala {
    scalaVersion = libs.versions.scala
}

dependencies {
    implementation(libs.scala.library)
    testImplementation(libs.scalatest)
    testImplementation(libs.scalatestplus.junit)
    testRuntimeOnly(libs.junit4)

    intellijPlatform {
        intellijIdea(libs.versions.intellij.platform)
    }
}

tasks {
    test {
        useJUnit()
    }
}
