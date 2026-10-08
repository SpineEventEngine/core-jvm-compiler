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

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.spine.tools.compiler.gradle.api.CompilerSettings
import io.spine.tools.compiler.gradle.plugin.Extension
import io.spine.tools.compiler.gradle.plugin.LaunchSpineCompiler
import io.spine.tools.compiler.jvm.style.JavaCodeStyle
import io.spine.tools.core.annotation.ApiAnnotationsPlugin
import io.spine.tools.core.jvm.gradle.GradleProjects.evaluate
import io.spine.tools.core.jvm.gradle.given.StubProject
import io.spine.tools.core.jvm.gradle.plugins.CompilerConfigPlugin.Companion.VALIDATION_PLUGIN_CLASS
import io.spine.tools.core.jvm.gradle.plugins.CompilerConfigPlugin.Companion.WRITE_COMPILER_PLUGINS_SETTINGS
import io.spine.tools.core.jvm.gradle.plugins.CoreJvmCompilerPlugins.API_ANNOTATIONS
import io.spine.tools.core.jvm.gradle.plugins.CoreJvmCompilerPlugins.COMPARABLE
import io.spine.tools.core.jvm.gradle.plugins.CoreJvmCompilerPlugins.ENTITY
import io.spine.tools.core.jvm.gradle.plugins.CoreJvmCompilerPlugins.MESSAGE_GROUP
import io.spine.tools.core.jvm.gradle.plugins.CoreJvmCompilerPlugins.SIGNAL
import io.spine.tools.core.jvm.gradle.plugins.CoreJvmCompilerPlugins.UUID
import io.spine.tools.core.jvm.gradle.plugins.WriteCompilerPluginsSettings.Companion.JAVA_CODE_STYLE_ID
import io.spine.tools.core.jvm.settings.Comparables
import io.spine.tools.core.jvm.settings.Entities
import io.spine.tools.core.jvm.settings.GroupSettings
import io.spine.tools.core.jvm.settings.SignalSettings
import io.spine.tools.core.jvm.settings.Uuids
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
import io.spine.tools.core.jvm.annotation.Settings as AnnotationPluginSettings

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
    fun `pass the settings of each Compiler plugin to the task as its input`() {
        val task = project.tasks.withType<WriteCompilerPluginsSettings>().single()
        val settingsTypes = task.settings.get().mapValues { (_, settings) -> settings::class }
        settingsTypes shouldBe mapOf(
            API_ANNOTATIONS to AnnotationPluginSettings::class,
            ENTITY to Entities::class,
            SIGNAL to SignalSettings::class,
            MESSAGE_GROUP to GroupSettings::class,
            UUID to Uuids::class,
            COMPARABLE to Comparables::class,
            JAVA_CODE_STYLE_ID to JavaCodeStyle::class
        )
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
