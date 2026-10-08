/*
 * Copyright 2026 CodeMatters, Lda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package io.spine.tools.core.jvm.gradle.plugins

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.spine.tools.core.jvm.gradle.Compiler
import io.spine.tools.core.jvm.gradle.module.ArtifactRegistry
import io.spine.tools.core.jvm.gradle.plugins.CompilerConfigPlugin.Companion.WRITE_COMPILER_PLUGINS_SETTINGS
import io.spine.tools.core.jvm.gradle.plugins.CoreJvmCompilerPlugins.API_ANNOTATIONS
import io.spine.tools.core.jvm.gradle.plugins.CoreJvmCompilerPlugins.COMPARABLE
import io.spine.tools.core.jvm.gradle.plugins.CoreJvmCompilerPlugins.ENTITY
import io.spine.tools.core.jvm.gradle.plugins.CoreJvmCompilerPlugins.MESSAGE_GROUP
import io.spine.tools.core.jvm.gradle.plugins.CoreJvmCompilerPlugins.SIGNAL
import io.spine.tools.core.jvm.gradle.plugins.CoreJvmCompilerPlugins.UUID
import io.spine.tools.core.jvm.gradle.plugins.WriteCompilerPluginsSettings.Companion.JAVA_CODE_STYLE_ID
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

@DisplayName("`CoreJvmPlugin` published as a Maven artifact should")
internal class CoreJvmPluginIgTest {

    companion object {

        private const val PROTOBUF_VERSION_PROPERTY = "protobuf.version"

        /**
         * The Protobuf runtime coordinate pinned by the fixtures.
         *
         * The version arrives through a system property set by the `test` task
         * in `gradle-plugin/build.gradle.kts`. Reading it unchecked would put
         * the literal `null` into the generated script, and the TestKit build
         * would fail on a coordinate that says nothing about the cause.
         */
        private val protobufJava: String =
            checkNotNull(System.getProperty(PROTOBUF_VERSION_PROPERTY)) {
                "System property `$PROTOBUF_VERSION_PROPERTY` is not set."
            }.let { "com.google.protobuf:protobuf-java:$it" }

        val repos = """
            |repositories {
            |    mavenLocal()
            |    maven { url = uri("${ArtifactRegistry.releases}") }
            |    maven { url = uri("${ArtifactRegistry.snapshots}") }
            |    mavenCentral()
            |}""".trimMargin()

        @Language("kotlin")
        private val buildscriptWithFullClasspath = """
            |buildscript {
            |    $repos
            |    configurations.all {
            |        // Pinned by the test, not by a consumer: this fixture
            |        // stands in for a published artifact, and the pin keeps
            |        // the plugin classpath off the pre-refresh runtime.
            |        resolutionStrategy.force("$protobufJava")
            |    }
            |    dependencies {
            |        classpath("${Meta.artifact.coordinates}")
            |        classpath("${Compiler.pluginLib.artifact.coordinates}")
            |    }
            |}
            |""".trimMargin()

        @Language("kotlin")
        private val buildscriptWithShortClasspath = """
            |buildscript {
            |    $repos
            |    configurations.all {
            |        // Pinned by the test, not by a consumer: this fixture
            |        // stands in for a published artifact, and the pin keeps
            |        // the plugin classpath off the pre-refresh runtime.
            |        resolutionStrategy.force("$protobufJava")
            |    }
            |    dependencies {
            |        classpath("${Compiler.pluginLib.artifact.coordinates}")
            |    }
            |}
            |""".trimMargin()

        /**
         * The extension of the settings files in the Protobuf JSON format.
         */
        private const val JSON = "pb.json"

        /**
         * The names of the settings files written for the Compiler plugins.
         */
        private val settingsFiles = listOf(
            API_ANNOTATIONS,
            ENTITY,
            SIGNAL,
            MESSAGE_GROUP,
            UUID,
            COMPARABLE,
            JAVA_CODE_STYLE_ID
        ).map { "$it.$JSON" }

        private const val INTERNAL_METHOD_NAME = "internalForIgTest"

        /**
         * The build script code which adds [INTERNAL_METHOD_NAME] to the options of
         * the CoreJvm Gradle Plugin.
         *
         * The options are nested under the `spine` extension, so the code obtains them by type.
         */
        @Language("kotlin")
        private val addingInternalMethodName = """
            |
            |val spine = extensions.getByName("spine") as ExtensionAware
            |spine.extensions.getByType<io.spine.tools.core.jvm.gradle.CoreJvmOptions>()
            |    .annotation.internalMethodNames.add("$INTERNAL_METHOD_NAME")
            |""".trimMargin()

        /**
         * The build file of a project that applies the CoreJvm Gradle Plugin by its ID.
         */
        @Language("kotlin")
        private val buildFileApplyingPluginById = buildscriptWithShortClasspath + """
            |plugins {
            |    java
            |    kotlin("jvm").version("${KotlinGradlePlugin.version}")
            |    id("${ProtobufGradlePlugin.id}") version "${ProtobufGradlePlugin.version}"
            |    id("${KspGradlePlugin.id}") version "${KspGradlePlugin.version}"
            |    id("io.spine.core-jvm") version "${Meta.artifact.version}"
            |}
            |
            |group = "io.spine.tools.tests"
            |version = "1.0.0-SNAPSHOT"
            |
            |$repos
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
            |        maven { url = uri("${ArtifactRegistry.releases}") }
            |        maven { url = uri("${ArtifactRegistry.snapshots}") }
            |        mavenCentral()
            |    }
            |}
            |""".trimMargin()
    }

    @Test
    fun `apply to a single-module project via classpath`(@TempDir projectDir: File) {
        @Language("kotlin")
        val buildFile = buildscriptWithFullClasspath + """
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
        val buildFile = buildscriptWithFullClasspath + """
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
        val project = GradleProject.setupAt(projectDir)
            .withSharedTestKitDirectory()
            .addFile("settings.gradle.kts", settingsWithRepositories.lines())
            .addFile("build.gradle.kts", buildFileApplyingPluginById.lines())
            .create()
        val task = BaseTaskName.build
        val result = project.executeTask(task)
        result[task] shouldBe TaskOutcome.SUCCESS
    }

    @Test
    fun `write settings of the Compiler plugins with the configuration cache`(
        @TempDir projectDir: File
    ) {
        val project = GradleProject.setupAt(projectDir)
            .withSharedTestKitDirectory()
            .withOptions("--configuration-cache")
            .addFile("settings.gradle.kts", settingsWithRepositories.lines())
            .addFile("build.gradle.kts", buildFileApplyingPluginById.lines())
            .create()
        val task = TaskName.of(WRITE_COMPILER_PLUGINS_SETTINGS)

        val first = project.executeTask(task)
        first[task] shouldBe TaskOutcome.SUCCESS
        first.output shouldContain "Configuration cache entry stored."
        projectDir.settingsDir().list().orEmpty().toList() shouldContainExactlyInAnyOrder
                settingsFiles

        val second = project.executeTask(task)
        second[task] shouldBe TaskOutcome.UP_TO_DATE
        second.output shouldContain "Configuration cache entry reused."
    }

    @Test
    fun `rewrite the settings when the options of the project change`(
        @TempDir projectDir: File
    ) {
        val project = GradleProject.setupAt(projectDir)
            .withSharedTestKitDirectory()
            .addFile("settings.gradle.kts", settingsWithRepositories.lines())
            .addFile("build.gradle.kts", buildFileApplyingPluginById.lines())
            .create()
        val task = TaskName.of(WRITE_COMPILER_PLUGINS_SETTINGS)
        project.executeTask(task)[task] shouldBe TaskOutcome.SUCCESS

        projectDir.resolve("build.gradle.kts").appendText(addingInternalMethodName)
        project.executeTask(task)[task] shouldBe TaskOutcome.SUCCESS

        val annotationSettings = projectDir.settingsDir().resolve("$API_ANNOTATIONS.$JSON")
        annotationSettings.readText() shouldContain INTERNAL_METHOD_NAME
    }
}

/**
 * Obtains the directory to which the CoreJvm Gradle Plugin writes the settings
 * of the Compiler plugins in the project located in this directory.
 */
private fun File.settingsDir(): File = resolve("build/spine/compiler/settings")
