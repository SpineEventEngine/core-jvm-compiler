/*
 * Copyright 2025, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.tools.core.jvm.gradle.plugins

import io.kotest.matchers.shouldBe
import io.spine.tools.core.jvm.gradle.module.ArtifactRegistry
import io.spine.tools.gradle.task.BaseTaskName
import io.spine.tools.gradle.task.TaskName
import io.spine.tools.gradle.testing.GradleProject
import io.spine.tools.gradle.testing.get
import java.io.File
import org.gradle.testkit.runner.TaskOutcome
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`CoreJvmPlugin` published as a fat JAR should")
class CoreJvmPluginIgTest {

    companion object {
        @Language("kotlin")
        private val buildscriptWithClasspathDependency = """
            |buildscript {
            |    repositories {
            |        mavenLocal()
            |        maven { url = uri("${ArtifactRegistry.releases}") }
            |        maven { url = uri("${ArtifactRegistry.snapshots}") }
            |        mavenCentral()
            |    }
            |    dependencies {
            |        classpath("io.spine.tools:core-jvm-plugins:${Meta.artifact.version}")
            |    }
            |}
            |""".trimMargin()

        @Language("kotlin")
        private val buildscript = """
            |buildscript {
            |    repositories {
            |        mavenLocal()
            |        maven { url = uri("${ArtifactRegistry.releases}") }
            |        maven { url = uri("${ArtifactRegistry.snapshots}") }
            |        mavenCentral()
            |    }
            |}
            |""".trimMargin()

        @Language("kotlin")
        private val settingsFile = """
            |rootProject.name = "core-jvm-plugin-ig-test"
        """.trimMargin()

        @Language("kotlin")
        val settingsWithRepositories = """
            |rootProject.name = "core-jvm-plugin-ig-test"
            |pluginManagement {
            |    repositories {
            |        gradlePluginPortal()
            |        mavenLocal()
            |        mavenCentral()
            |    }
            |}
            |""".trimMargin()
    }

    @Test
    fun `apply to a single-module project via classpath`(@TempDir projectDir: File) {
        @Language("kotlin")
        val buildFile = buildscriptWithClasspathDependency + """
            |plugins {
            |    java
            |    kotlin("jvm").version("${KotlinGradlePlugin.version}")
            |    id("com.google.protobuf").version("${ProtobufGradlePlugin.version}")
            |}
            |
            |apply(plugin = "io.spine.core-jvm")
            |
            |group = "io.spine.tools.tests"
            |version = "1.0.0-SNAPSHOT"
            |
            |tasks.register("verify") {
            |    doLast {
            |        println("`CoreJvmPlugin` applied via `classpath` successfully.")
            |    }
            |}
            |""".trimMargin()
        val project = GradleProject.setupAt(projectDir)
            .withSharedTestKitDirectory()
            .addFile("settings.gradle.kts", settingsFile.lines())
            .addFile("build.gradle.kts", buildFile.lines())
            .create()
        val verify = TaskName.of("verify")
        val result = project.executeTask(verify)
        result[verify] shouldBe TaskOutcome.SUCCESS
    }

    @Test
    fun `apply Protobuf Gradle Plugin automatically, if not yet applied`(
        @TempDir projectDir: File
    ) {
        @Language("kotlin")
        val buildFile = buildscriptWithClasspathDependency + """
            |plugins {
            |    java
            |    kotlin("jvm").version("${KotlinGradlePlugin.version}")
            |}
            |
            |apply(plugin = "io.spine.core-jvm")
            |
            |group = "io.spine.tools.tests"
            |version = "1.0.0-SNAPSHOT"
            |
            |tasks.register("verify") {
            |    doLast {
            |        println("`CoreJvmPlugin` applied via `classpath` successfully.")
            |    }
            |}
            |""".trimMargin()
        val project = GradleProject.setupAt(projectDir)
            .withSharedTestKitDirectory()
            .addFile("settings.gradle.kts", settingsFile.lines())
            .addFile("build.gradle.kts", buildFile.lines())
            .create()
        val verify = TaskName.of("verify")
        val result = project.executeTask(verify)
        result[verify] shouldBe TaskOutcome.SUCCESS
    }

    @Test
    fun `be available via its ID and version`(@TempDir projectDir: File) {
        @Language("kotlin")
        val buildFile = buildscript + """
            |plugins {
            |    java
            |    kotlin("jvm").version("${KotlinGradlePlugin.version}")
            |    id("com.google.protobuf") version "${ProtobufGradlePlugin.version}"
            |    id("io.spine.core-jvm") version "${Meta.artifact.version}"
            |}
            |
            |group = "io.spine.tools.tests"
            |version = "1.0.0-SNAPSHOT"
            |
            |repositories {
            |    mavenLocal()
            |    maven { url = uri("${ArtifactRegistry.releases}") }
            |    maven { url = uri("${ArtifactRegistry.snapshots}") }
            |    mavenCentral()
            |}
            |
            |""".trimMargin()

        val project = GradleProject.setupAt(projectDir)
            .withSharedTestKitDirectory()
            .addFile("settings.gradle.kts", settingsWithRepositories.lines())
            .addFile("build.gradle.kts", buildFile.lines())
            .create()
        val task = BaseTaskName.build
        val result = project.executeTask(task)
        result[task] shouldBe TaskOutcome.SUCCESS
    }
}
