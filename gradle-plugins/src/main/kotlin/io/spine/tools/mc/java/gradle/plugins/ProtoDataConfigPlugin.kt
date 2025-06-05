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

package io.spine.tools.mc.java.gradle.plugins

import io.spine.protodata.gradle.Names
import io.spine.protodata.gradle.Names.GRADLE_PLUGIN_ID
import io.spine.protodata.gradle.plugin.LaunchProtoData
import io.spine.protodata.java.style.JavaCodeStyleFormatterPlugin
import io.spine.protodata.gradle.protoDataWorkingDir
import io.spine.protodata.params.WorkingDirectory
import io.spine.tools.fs.DirectoryName
import io.spine.tools.gradle.Artifact
import io.spine.tools.mc.annotation.ApiAnnotationsPlugin
import io.spine.tools.mc.java.comparable.ComparablePlugin
import io.spine.tools.mc.java.entity.EntityPlugin
import io.spine.tools.mc.java.gradle.McJava.allPlugins
import io.spine.tools.mc.java.gradle.ValidationSdk
import io.spine.tools.mc.java.gradle.generatedGrpcDirName
import io.spine.tools.mc.java.gradle.generatedJavaDirName
import io.spine.tools.mc.java.gradle.mcJava
import io.spine.tools.mc.java.gradle.plugins.ProtoDataConfigPlugin.Companion.VALIDATION_PLUGIN_CLASS
import io.spine.tools.mc.java.gradle.plugins.ProtoDataConfigPlugin.Companion.WRITE_PROTODATA_SETTINGS
import io.spine.tools.mc.java.gradle.settings.CodegenSettings
import io.spine.tools.mc.java.marker.MarkerPlugin
import io.spine.tools.mc.java.mgroup.MessageGroupPlugin
import io.spine.tools.mc.java.signal.SignalPlugin
import io.spine.tools.mc.java.signal.rejection.RThrowablePlugin
import io.spine.tools.mc.java.uuid.UuidPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import io.spine.protodata.gradle.CodegenSettings as ProtoDataSettings
import io.spine.protodata.plugin.Plugin as ProtoDataPlugin

/**
 * The plugin that configures ProtoData for the associated project.
 *
 * This plugin does the following:
 *   1. Applies the `io.spine.protodata` Gradle plugin to the project.
 *   2. Configures the ProtoData extension of a Gradle project, passing codegen
 *      plugins of ProtoData, such
 *      as [JavaValidationPlugin][io.spine.validation.java.JavaValidationPlugin].
 *   3. Creates a [WriteProtoDataSettings] task for passing configuration to ProtoData, and
 *      links it to the [LaunchProtoData] task.
 *   4. Adds required dependencies.
 */
internal class ProtoDataConfigPlugin : Plugin<Project> {

    /**
     * Applies the `io.spine.protodata` plugin to the project and, if the user needs
     * validation code generation, configures ProtoData to generate Java validation code.
     *
     * ProtoData configuration is a tricky operation because of Gradle's lifecycle.
     * We need to squeeze our configuration before the `LaunchProtoData` task is configured.
     * This means adding the `afterEvaluate(..)` hook before the ProtoData Gradle plugin
     * is applied to the project.
     */
    override fun apply(project: Project) {
        project.afterEvaluate {
            it.configureProtoData()
        }
        project.pluginManager.apply(GRADLE_PLUGIN_ID)
    }

    companion object {

        /**
         * The name of the task for writing ProtoData settings.
         */
        const val WRITE_PROTODATA_SETTINGS = "writeProtoDataSettings"

        /**
         * The name of the Validation plugin for ProtoData.
         */
        const val VALIDATION_PLUGIN_CLASS = "io.spine.validation.java.JavaValidationPlugin"
    }
}

private fun Project.configureProtoData() {
    configureProtoDataPlugins()
    val writeSettingsTask = createWriteSettingsTask()
    tasks.withType<LaunchProtoData>().all { task ->
        task.apply {
            dependsOn(writeSettingsTask)
            setStandardOutput(System.out)
            setErrorOutput(System.err)
        }
    }
}

private fun Project.createWriteSettingsTask(): Provider<WriteProtoDataSettings> {
    val result = tasks.register<WriteProtoDataSettings>(WRITE_PROTODATA_SETTINGS) {
        val workingDir = WorkingDirectory(protoDataWorkingDir.asFile.toPath())
        val settingsDir = workingDir.settingsDirectory.path.toFile()
        val settingsDirProvider = project.layout.dir(provider { settingsDir })
        this.settingsDir.set(settingsDirProvider)
    }
    return result
}

/**
 * Configures ProtoData with plugins for the given Gradle project.
 */
private fun Project.configureProtoDataPlugins() {
    // Pass the uber JAR of McJava so that plugins from all the submodules are available.
    addUserClasspathDependency(allPlugins)

    val protodata = extensions.getByType<ProtoDataSettings>()
    protodata.setSubdirectories()

    configureValidation(protodata)
    configureSignals(protodata)

    protodata.run {
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

private val Project.messageOptions: CodegenSettings
    get() = mcJava.codegen!!

private fun ProtoDataSettings.setSubdirectories() {
    subDirs = listOf(
        generatedJavaDirName.value(),
        generatedGrpcDirName.value(),
        DirectoryName.kotlin.value()
    )
}

private fun Project.configureValidation(protodata: ProtoDataSettings) {
    val validationConfig = messageOptions.validation
    val version = validationConfig.version.get()
    if (validationConfig.enabled.get()) {
        addUserClasspathDependency(ValidationSdk.javaCodegenBundle(version))
        protodata.plugins(
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

private fun Project.configureSignals(protodata: ProtoDataSettings) {
    protodata.addPlugin<SignalPlugin>()

    val rejectionCodegen = messageOptions.rejections
    if (rejectionCodegen.enabled.get()) {
        protodata.addPlugin<RThrowablePlugin>()
    }
}

private fun Project.addUserClasspathDependency(vararg artifacts: Artifact) = artifacts.forEach {
    addDependency(Names.USER_CLASSPATH_CONFIGURATION, it)
}

private fun Project.addDependency(configuration: String, artifact: Artifact) {
    val dependency = findDependency(artifact) ?: artifact.notation()
    dependencies.add(configuration, dependency)
}

private fun Project.findDependency(artifact: Artifact): Dependency? {
    val dependencies = configurations.flatMap { c -> c.dependencies }
    val found = dependencies.firstOrNull { d ->
        artifact.group() == d.group // `d.group` could be `null`.
                && artifact.name() == d.name
    }
    return found
}

private inline fun <reified T : ProtoDataPlugin> ProtoDataSettings.addPlugin() {
    plugins(T::class.java.name)
}
