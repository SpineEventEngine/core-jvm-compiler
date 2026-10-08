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

@file:Suppress("TooManyFunctions") // Prefer smaller configuration steps as `fun` over the limit.

package io.spine.tools.core.jvm.gradle.plugins

import io.spine.tools.compiler.gradle.api.CompilerSettings
import io.spine.tools.compiler.gradle.api.addUserClasspathDependency
import io.spine.tools.compiler.gradle.api.compilerSettings
import io.spine.tools.compiler.gradle.api.compilerWorkingDir
import io.spine.tools.compiler.gradle.plugin.LaunchSpineCompiler
import io.spine.tools.compiler.jvm.style.JavaCodeStyleFormatterPlugin
import io.spine.tools.compiler.params.WorkingDirectory
import io.spine.tools.core.jvm.gradle.CoreJvmCompiler
import io.spine.tools.core.jvm.gradle.coreJvmOptions
import io.spine.tools.core.jvm.gradle.generatedGrpcDirName
import io.spine.tools.core.jvm.gradle.generatedJavaDirName
import io.spine.tools.core.jvm.gradle.plugins.CompilerConfigPlugin.Companion.WRITE_COMPILER_PLUGINS_SETTINGS
import io.spine.tools.core.jvm.gradle.plugins.CoreJvmCompilerPlugins.API_ANNOTATIONS
import io.spine.tools.core.jvm.gradle.plugins.CoreJvmCompilerPlugins.COMPARABLE
import io.spine.tools.core.jvm.gradle.plugins.CoreJvmCompilerPlugins.ENTITY
import io.spine.tools.core.jvm.gradle.plugins.CoreJvmCompilerPlugins.MARKER
import io.spine.tools.core.jvm.gradle.plugins.CoreJvmCompilerPlugins.MESSAGE_GROUP
import io.spine.tools.core.jvm.gradle.plugins.CoreJvmCompilerPlugins.REJECTION_THROWABLE
import io.spine.tools.core.jvm.gradle.plugins.CoreJvmCompilerPlugins.SIGNAL
import io.spine.tools.core.jvm.gradle.plugins.CoreJvmCompilerPlugins.UUID
import io.spine.tools.core.jvm.gradle.settings.CoreJvmCompilerSettings
import io.spine.tools.fs.DirectoryName
import io.spine.tools.gradle.task.JavaTaskName.Companion.processResources
import io.spine.tools.gradle.task.JavaTaskName.Companion.sourcesJar
import io.spine.tools.gradle.task.SpineTaskGroup
import io.spine.tools.validation.gradle.ValidationGradlePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import io.spine.tools.compiler.gradle.plugin.Plugin as CompilerGradlePlugin
import io.spine.tools.compiler.plugin.Plugin as CompilerPlugin

/**
 * The plugin that configures the Spine Compiler for the associated project.
 *
 * This plugin does the following:
 *   1. Applies the `io.spine.compiler` Gradle plugin to the project.
 *   2. Configures the Compiler extension of the Gradle project, passing the compiler plugins,
 *      introduced by the modules of the CoreJvm Compiler modules.
 *   3. Creates a [WriteCompilerPluginsSettings] task for passing configuration to the Compiler, and
 *      links it to the [LaunchSpineCompiler] task.
 *   4. Adds required dependencies.
 */
internal class CompilerConfigPlugin : Plugin<Project> {

    /**
     * Applies the `io.spine.compiler` plugin to the project and, if the user needs
     * validation code generation, configures the Compiler to generate Java validation code.
     *
     * Spine Compiler configuration is a tricky operation because of Gradle's lifecycle.
     * We need to squeeze our configuration before the `LaunchSpineCompiler` task is configured.
     * This means adding the `afterEvaluate(..)` hook before the Compiler Gradle plugin
     * is applied to the project.
     */
    override fun apply(project: Project) {
        project.afterEvaluate {
            it.configureCompiler()
        }
        // Apply the Compiler Gradle Plugin so that we can manipulate the compiler settings.
        // We do not want the user to add it manually.
        project.apply<CompilerGradlePlugin>()
    }

    companion object {

        /**
         * The name of the task for writing the settings of CoreJvm Compiler plugins.
         */
        const val WRITE_COMPILER_PLUGINS_SETTINGS = "writeSpineCompilerPluginsSettings"

        /**
         * The name of the Validation plugin for the Compiler.
         */
        const val VALIDATION_PLUGIN_CLASS = "io.spine.tools.validation.java.JavaValidationPlugin"
    }
}

private fun Project.configureCompiler() {
    configureCompilerPlugins()
    val writeSettingsTask = createWriteSettingsTask()
    tasks.withType<LaunchSpineCompiler>().configureEach { task ->
        task.apply {
            dependsOn(writeSettingsTask)
            standardOutput = System.out
            errorOutput = System.err
        }
    }
    // Make `processResources` and `sourcesJar` run after `writeSpineCompilerPluginsSettings`
    // as demanded by Gradle 9.x. The settings task does not produce resources or sources,
    // but we want to avoid forcing users set the dependencies manually in their projects.
    // Filtering by name does not realize the tasks, and skips those absent in the project.
    val runAfterSettings = setOf(processResources.value(), sourcesJar.value())
    tasks.named { it in runAfterSettings }.configureEach {
        it.mustRunAfter(writeSettingsTask)
    }
}

private fun Project.createWriteSettingsTask(): Provider<WriteCompilerPluginsSettings> {
    // Gradle evaluates the provider lazily, when it needs the task inputs.
    // The configuration cache stores the resulting value rather than the lambda,
    // which captures the `options`.
    val options = coreJvmOptions
    val pluginSettings = provider { options.compilerPluginSettings() }
    val result = tasks.register<WriteCompilerPluginsSettings>(WRITE_COMPILER_PLUGINS_SETTINGS) {
        group = SpineTaskGroup.name
        description = "Writes settings for Spine Compiler plugins of this project"

        val workingDir = WorkingDirectory(compilerWorkingDir.asFile.toPath())
        settingsDir.set(workingDir.settingsDirectory.path.toFile())
        settings.set(pluginSettings)
    }
    return result
}

/**
 * Configures the Compiler with plugins for the given Gradle project.
 */
private fun Project.configureCompilerPlugins() {
    // Pass the fat JAR of the CoreJvm Compiler plugins so that the plugins from
    // all the feature modules are available to the Compiler.
    // The fat JAR is always published with the same version as this plugin.
    addUserClasspathDependency(CoreJvmCompiler.fatJar(Meta.artifact.version))

    val compiler = compilerSettings
    compiler.setSubdirectories()

    // The Validation plugin must be applied first so that the Validation Compiler plugin
    // comes first in the pipeline.
    pluginManager.apply(ValidationGradlePlugin::class.java)

    configureSignals(compiler)

    compiler.run {
        addPlugin(MARKER)
        addPlugin(MESSAGE_GROUP)
        addPlugin(UUID)
        addPlugin(COMPARABLE)
        addPlugin(ENTITY)

        // Annotations should follow the signal and entity plugins
        // so that their output is annotated too.
        addPlugin(API_ANNOTATIONS)

        // The Java style formatting comes last to conclude all the rendering.
        addPlugin<JavaCodeStyleFormatterPlugin>()
    }
}

private val Project.messageOptions: CoreJvmCompilerSettings
    get() = checkNotNull(coreJvmOptions.compiler) {
        "The `compiler` options are not set." +
                " `CoreJvmPlugin` must call `CoreJvmOptions.injectProject()` first."
    }

private fun CompilerSettings.setSubdirectories() {
    subDirs = listOf(
        generatedJavaDirName.value(),
        generatedGrpcDirName.value(),
        DirectoryName.kotlin.value()
    )
}

private fun Project.configureSignals(compiler: CompilerSettings) {
    compiler.addPlugin(SIGNAL)

    val rejectionCodegen = messageOptions.rejections
    if (rejectionCodegen.enabled.get()) {
        compiler.addPlugin(REJECTION_THROWABLE)
    }
}

private fun CompilerSettings.addPlugin(className: String) {
    plugins(className)
}

private inline fun <reified T : CompilerPlugin> CompilerSettings.addPlugin() {
    addPlugin(T::class.java.name)
}
