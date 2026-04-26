/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("scala")
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
    intellijPlatform {
        intellijIdea(libs.versions.intellij.platform)
    }
}

tasks {
    withType<ScalaCompile> {
        scalaCompileOptions.additionalParameters = listOf(
            "-deprecation", "-feature", "-unchecked"
        )
    }
}
