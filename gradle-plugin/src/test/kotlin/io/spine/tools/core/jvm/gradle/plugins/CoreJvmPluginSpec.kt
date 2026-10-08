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

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldNotBe
import io.spine.tools.compiler.gradle.api.CompilerSettings
import io.spine.tools.compiler.gradle.plugin.Extension
import io.spine.tools.compiler.gradle.plugin.LaunchSpineCompiler
import io.spine.tools.core.annotation.ApiAnnotationsPlugin
import io.spine.tools.core.jvm.gradle.GradleProjects.evaluate
import io.spine.tools.core.jvm.gradle.given.StubProject
import io.spine.tools.core.jvm.gradle.plugins.CompilerConfigPlugin.Companion.VALIDATION_PLUGIN_CLASS
import io.spine.tools.core.jvm.gradle.plugins.CompilerConfigPlugin.Companion.WRITE_COMPILER_PLUGINS_SETTINGS
import io.spine.tools.core.jvm.signal.rejection.RThrowablePlugin
import io.spine.tools.gradle.lib.spineExtension
import io.spine.tools.gradle.task.JavaTaskName.Companion.processResources
import io.spine.tools.gradle.task.JavaTaskName.Companion.sourcesJar
import io.spine.tools.gradle.testing.GradleProject
import java.io.File
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`CoreJvmPlugin` should")
internal class CoreJvmPluginSpec {

    companion object {

        lateinit var project: Project
        lateinit var compilerSettings: Extension

        @BeforeAll
        @JvmStatic
        fun createProjectAndPlugins(@TempDir tempDir: File) {
            project = StubProject.createIn(tempDir)
                .withMavenRepositories()
                .get()

            val plugins = project.pluginManager
            plugins.apply(GradleProject.javaPlugin)
            plugins.apply("com.google.protobuf")
            plugins.apply(CoreJvmPlugin::class.java)

            evaluate(project)

            // Register `sourcesJar` only after the evaluation, when the plugin has
            // already configured the Compiler, as a plugin applied later would do.
            project.extensions.getByType<JavaPluginExtension>().withSourcesJar()

            compilerSettings = project.spineExtension<CompilerSettings>() as Extension
        }
    }

    @Test
    fun `add project extension`() {
        compilerSettings shouldNotBe null
    }

    @Test
    fun `add Compiler plugins`() {
        val plugins = compilerSettings.plugins.get()
        plugins.shouldContainInOrder(
            VALIDATION_PLUGIN_CLASS,
            RThrowablePlugin::class.java.name,
            ApiAnnotationsPlugin::class.java.name
        )
    }

    @Test
    fun `add a task for passing configuration file`() {
        val task = project.tasks.withType<WriteCompilerPluginsSettings>()
        task shouldNotBe null
        task.shouldNotBeEmpty()
    }

    @Test
    fun `add a task for launching the Compiler CLI`() {
        val task = project.tasks.withType<LaunchSpineCompiler>()
        task shouldNotBe null
        task.shouldNotBeEmpty()
    }

    @Test
    fun `make the Compiler launch tasks depend on writing the settings`() {
        val tasks = project.tasks.withType<LaunchSpineCompiler>()
        tasks.shouldNotBeEmpty()
        tasks.forEach { task ->
            val dependencies = task.taskDependencies.getDependencies(task).map { it.name }
            dependencies shouldContain WRITE_COMPILER_PLUGINS_SETTINGS
        }
    }

    @Test
    fun `make processResources run after writing the settings`() {
        val task = project.tasks.getByName(processResources.value())
        task.mustRunAfterNames() shouldContain WRITE_COMPILER_PLUGINS_SETTINGS
    }

    @Test
    fun `make sourcesJar run after writing the settings, even if registered later`() {
        val task = project.tasks.getByName(sourcesJar.value())
        task.mustRunAfterNames() shouldContain WRITE_COMPILER_PLUGINS_SETTINGS
    }
}

private fun Task.mustRunAfterNames(): List<String> =
    mustRunAfter.getDependencies(this).map { it.name }
