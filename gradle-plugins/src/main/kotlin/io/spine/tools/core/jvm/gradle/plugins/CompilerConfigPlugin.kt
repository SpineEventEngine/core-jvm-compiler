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
import io.spine.tools.compiler.gradle.api.Names
import io.spine.tools.compiler.gradle.api.Names.GRADLE_PLUGIN_ID
import io.spine.tools.compiler.gradle.api.compilerSettings
import io.spine.tools.compiler.gradle.api.compilerWorkingDir
import io.spine.tools.compiler.gradle.plugin.LaunchSpineCompiler
import io.spine.tools.compiler.jvm.style.JavaCodeStyleFormatterPlugin
import io.spine.tools.compiler.params.WorkingDirectory
import io.spine.tools.core.annotation.ApiAnnotationsPlugin
import io.spine.tools.core.jvm.comparable.ComparablePlugin
import io.spine.tools.core.jvm.entity.EntityPlugin
import io.spine.tools.core.jvm.gradle.CoreJvmCompiler
import io.spine.tools.core.jvm.gradle.ValidationSdk
import io.spine.tools.core.jvm.gradle.coreJvmOptions
import io.spine.tools.core.jvm.gradle.generatedGrpcDirName
import io.spine.tools.core.jvm.gradle.generatedJavaDirName
import io.spine.tools.core.jvm.gradle.plugins.CompilerConfigPlugin.Companion.VALIDATION_PLUGIN_CLASS
import io.spine.tools.core.jvm.gradle.plugins.CompilerConfigPlugin.Companion.WRITE_COMPILER_SETTINGS
import io.spine.tools.core.jvm.gradle.settings.CoreJvmCompilerSettings
import io.spine.tools.core.jvm.marker.MarkerPlugin
import io.spine.tools.core.jvm.mgroup.MessageGroupPlugin
import io.spine.tools.core.jvm.signal.SignalPlugin
import io.spine.tools.core.jvm.signal.rejection.RThrowablePlugin
import io.spine.tools.core.jvm.uuid.UuidPlugin
import io.spine.tools.fs.DirectoryName
import io.spine.tools.meta.MavenArtifact
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import io.spine.tools.compiler.plugin.Plugin as CompilerPlugin

/**
 * The plugin that configures the Spine Compiler for the associated project.
 *
 * This plugin does the following:
 *   1. Applies the `io.spine.compiler` Gradle plugin to the project.
 *   2. Configures the Compiler extension of the Gradle project, passing the compiler plugins,
 *      such as [JavaValidationPlugin][io.spine.validation.java.JavaValidationPlugin] and
 *      the plugins introduced by the modules of the CoreJvm Compiler modules.
 *   3. Creates a [WriteCompilerSettings] task for passing configuration to the Compiler, and
 *      links it to the [LaunchSpineCompiler] task.
 *   4. Adds required dependencies.
 */
internal class CompilerConfigPlugin : Plugin<Project> {

    /**
     * Applies the `io.spine.compiler` plugin to the project and, if the user needs
     * validation code generation, configures the Compiler to generate Java validation code.
     *
     * ProtoData configuration is a tricky operation because of Gradle's lifecycle.
     * We need to squeeze our configuration before the `LaunchProtoData` task is configured.
     * This means adding the `afterEvaluate(..)` hook before the ProtoData Gradle plugin
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
         * The name of the task for writing the Compiler settings.
         */
        const val WRITE_COMPILER_SETTINGS = "writeSpineCompilerSettings"

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
            setStandardOutput(System.out)
            setErrorOutput(System.err)
        }
    }
}

private fun Project.createWriteSettingsTask(): Provider<WriteCompilerSettings> {
    val result = tasks.register<WriteCompilerSettings>(WRITE_COMPILER_SETTINGS) {
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
    addUserClasspathDependency(CoreJvmCompiler.artifact)

    val compiler = compilerSettings
    compiler.setSubdirectories()

    configureValidation(compiler)
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

private fun Project.configureValidation(compiler: CompilerSettings) {
    val validationConfig = messageOptions.validation
    val version = validationConfig.version.get()
    if (validationConfig.enabled.get()) {
        addUserClasspathDependency(ValidationSdk.javaCodegenBundle(version))
        compiler.plugins(
            VALIDATION_PLUGIN_CLASS
        )
    } else {
        addUserClasspathDependency(ValidationSdk.configuration(version))
    }

    // We add the dependency on runtime anyway for the following reasons:
    //  1. We do not want users to change their Gradle build files when they turn on or off
    //     code generation for the validation code.
    //
    //  2. We have run-time validation rules that are going to be used in parallel with
    //     the generated code. This includes current and new implementation for validation
    //     rules for the already existing generated Protobuf code.
    //
    addDependency("implementation", ValidationSdk.javaRuntime(version))
}

private fun Project.configureSignals(compiler: CompilerSettings) {
    compiler.addPlugin<SignalPlugin>()

    val rejectionCodegen = messageOptions.rejections
    if (rejectionCodegen.enabled.get()) {
        compiler.addPlugin<RThrowablePlugin>()
    }
}

private fun Project.addUserClasspathDependency(vararg artifacts: MavenArtifact) =
    artifacts.forEach {
        addDependency(Names.USER_CLASSPATH_CONFIGURATION, it)
    }

private fun Project.addDependency(configuration: String, artifact: MavenArtifact) {
    val dependency = findDependency(artifact) ?: artifact.coordinates
    dependencies.add(configuration, dependency)
}

private fun Project.findDependency(artifact: MavenArtifact): Dependency? {
    val dependencies = configurations.flatMap { c -> c.dependencies }
    val found = dependencies.firstOrNull { d ->
        artifact.group == d.group // `d.group` could be `null`.
                && artifact.name == d.name
    }
    return found
}

private inline fun <reified T : CompilerPlugin> CompilerSettings.addPlugin() {
    plugins(T::class.java.name)
}
