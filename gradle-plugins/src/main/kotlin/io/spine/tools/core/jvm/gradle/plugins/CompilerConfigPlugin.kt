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

@file:Suppress("TooManyFunctions") // Prefer smaller configuration steps as `fun` over the limit.

package io.spine.tools.core.jvm.gradle.plugins

import io.spine.tools.compiler.gradle.api.CompilerSettings
import io.spine.tools.compiler.gradle.api.Names.GRADLE_PLUGIN_ID
import io.spine.tools.compiler.gradle.api.addUserClasspathDependency
import io.spine.tools.compiler.gradle.api.compilerSettings
import io.spine.tools.compiler.gradle.api.compilerWorkingDir
import io.spine.tools.compiler.gradle.plugin.LaunchSpineCompiler
import io.spine.tools.compiler.jvm.style.JavaCodeStyleFormatterPlugin
import io.spine.tools.compiler.params.WorkingDirectory
import io.spine.tools.core.annotation.ApiAnnotationsPlugin
import io.spine.tools.core.jvm.comparable.ComparablePlugin
import io.spine.tools.core.jvm.entity.EntityPlugin
import io.spine.tools.core.jvm.gradle.coreJvmOptions
import io.spine.tools.core.jvm.gradle.generatedGrpcDirName
import io.spine.tools.core.jvm.gradle.generatedJavaDirName
import io.spine.tools.core.jvm.gradle.plugins.CompilerConfigPlugin.Companion.WRITE_COMPILER_PLUGINS_SETTINGS
import io.spine.tools.core.jvm.gradle.settings.CoreJvmCompilerSettings
import io.spine.tools.core.jvm.marker.MarkerPlugin
import io.spine.tools.core.jvm.mgroup.MessageGroupPlugin
import io.spine.tools.core.jvm.signal.SignalPlugin
import io.spine.tools.core.jvm.signal.rejection.RThrowablePlugin
import io.spine.tools.core.jvm.uuid.UuidPlugin
import io.spine.tools.fs.DirectoryName
import io.spine.tools.gradle.task.JavaTaskName.Companion.processResources
import io.spine.tools.gradle.task.JavaTaskName.Companion.sourcesJar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import io.spine.tools.compiler.plugin.Plugin as CompilerPlugin

/**
 * The plugin that configures the Spine Compiler for the associated project.
 */
internal class CompilerConfigPlugin : Plugin<Project> {

    /**
     * Applies the `io.spine.compiler` plugin to the project and schedules
     * configuration of the Compiler on `afterEvaluate`.
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
        project.pluginManager.apply(GRADLE_PLUGIN_ID)
    }

    companion object {

        /**
         * The name of the task for writing the settings of CoreJvm Compiler plugins.
         */
        const val WRITE_COMPILER_PLUGINS_SETTINGS = "writeSpineCompilerPluginsSettings"

        /**
         * The name of the Validation plugin for ProtoData.
         */
        const val VALIDATION_PLUGIN_CLASS = "io.spine.validation.java.JavaValidationPlugin"
    }
}

private fun Project.configureCompiler() {
    configureCompilerPlugins()
    val writeSettingsTask = createWriteSettingsTask()
    tasks.withType<LaunchSpineCompiler>().all { task ->
        task.apply {
            dependsOn(writeSettingsTask)
            standardOutput = System.out
            errorOutput = System.err
        }
    }
    // Make `processResources` and `sourceJar` depend on `writeSpineCompilerPluginsSettings`
    // as demanded by Gradle 9.x. The settings task does not produce resources or sources,
    // but we want to avoid forcing users set the dependencies manually in their projects.
    tasks.findByName(processResources.value())?.mustRunAfter(writeSettingsTask)
    tasks.findByName(sourcesJar.value())?.mustRunAfter(writeSettingsTask)
}

private fun Project.createWriteSettingsTask(): Provider<WriteCompilerPluginsSettings> {
    val result = tasks.register<WriteCompilerPluginsSettings>(WRITE_COMPILER_PLUGINS_SETTINGS) {
        val workingDir = WorkingDirectory(compilerWorkingDir.asFile.toPath())
        val settingsDir = workingDir.settingsDirectory.path.toFile()
        val settingsDirProvider = project.layout.dir(provider { settingsDir })
        this.settingsDir.set(settingsDirProvider)
    }
    return result
}

/**
 * Configures the Compiler with plugins for the given Gradle project.
 */
private fun Project.configureCompilerPlugins() {
    // Pass the uber JAR of the CoreJvm Compiler Plugins so that plugins from
    // all the submodules are available.
    addUserClasspathDependency(Meta.artifact)

    val compiler = compilerSettings
    compiler.setSubdirectories()

    configureSignals(compiler)

    compiler.run {
        addPlugin<MarkerPlugin>()
        addPlugin<MessageGroupPlugin>()
        addPlugin<UuidPlugin>()
        addPlugin<ComparablePlugin>()
        addPlugin<EntityPlugin>()

        // Annotations should follow `SignalPlugin` and `EntityPlugin`
        // so that their output is annotated too.
        addPlugin<ApiAnnotationsPlugin>()

        // The Java style formatting comes last to conclude all the rendering.
        addPlugin<JavaCodeStyleFormatterPlugin>()
    }
}

private val Project.messageOptions: CoreJvmCompilerSettings
    get() = coreJvmOptions.compiler!!

private fun CompilerSettings.setSubdirectories() {
    subDirs = listOf(
        generatedJavaDirName.value(),
        generatedGrpcDirName.value(),
        DirectoryName.kotlin.value()
    )
}

private fun Project.configureSignals(compiler: CompilerSettings) {
    compiler.addPlugin<SignalPlugin>()

    val rejectionCodegen = messageOptions.rejections
    if (rejectionCodegen.enabled.get()) {
        compiler.addPlugin<RThrowablePlugin>()
    }
}

private inline fun <reified T : CompilerPlugin> CompilerSettings.addPlugin() {
    plugins(T::class.java.name)
}
