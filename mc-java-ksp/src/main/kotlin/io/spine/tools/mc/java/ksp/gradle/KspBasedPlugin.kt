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

package io.spine.tools.mc.java.ksp.gradle

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.gradle.KspExtension
import io.spine.protodata.gradle.ProtoDataTaskName
import io.spine.tools.code.SourceSetName
import io.spine.tools.fs.DirectoryName.grpc
import io.spine.tools.fs.DirectoryName.java
import io.spine.tools.fs.DirectoryName.kotlin
import io.spine.tools.gradle.project.sourceSets
import io.spine.tools.gradle.project.sourceSet
import io.spine.tools.gradle.protobuf.generated
import io.spine.tools.gradle.protobuf.generatedDir
import io.spine.tools.gradle.protobuf.generatedSourceProtoDir
import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.findByType

/**
 * Configures a Gradle project to run [KSP](https://kotlinlang.org/docs/ksp-overview.html) with
 * a processor specified by the [mavenCoordinates] property.
 *
 * The plugin performs the following configuration steps:
 *
 *  1. Adds the [KSP Gradle Plugin](https://github.com/google/ksp) to the project
 *     if it is not added already.
 *
 *  2. Makes a KSP task depend on a `LaunchProtoData` task for the same source set.
 *
 *  3. Replaces the output of KSP tasks to place the generated code into [Project.generatedDir]
 *     instead of the one under the `build` directory which is used by default.
 */
public abstract class KspBasedPlugin : Plugin<Project> {

    /**
     * The Maven coordinates of the plugin to be added to KSP configurations
     * in the project to which the plugin is applied.
     */
    protected abstract val mavenCoordinates: String

    override fun apply(project: Project) {
        project.run {
            pluginManager.withPlugin(KspGradlePlugin.id) {
                applyCommonSettings()
                addPluginToKspConfigurations()
            }
            applyKspPlugin()
        }
    }

    /**
     * Applies tunings common to all KSP-based plugins to this project,
     * unless it was [already done][commonSettingsApplied].
     */
    private fun Project.applyCommonSettings() {
        synchronized(lock) {
            if (!commonSettingsApplied.contains(this)) {
                useKsp2()
                addDependencies()
                excludeSourcesFromBuildDir()
                addProtoDataGeneratedSources()
                makeKspTasksDependOnProtoData()
                replaceKspOutputDirs()
                commonSettingsApplied.add(this)
            }
        }
    }

    private fun Project.addPluginToKspConfigurations() {
        configurations
            .filter { it.name.startsWith(configurationNamePrefix) }
            .forEach { kspConfiguration ->
                project.dependencies.add(kspConfiguration.name, mavenCoordinates)
            }
    }

    private fun Project.addDependencies() {
        sourceSets.forEach { sourceSet ->
            val configurationName = sourceSet.compileOnlyConfigurationName
            dependencies.add(configurationName,
                autoServiceAnnotations
            )
        }
    }

    @Suppress("ConstPropertyName")
    protected companion object {

        /**
         * The synchronization lock used by [applyCommonSettings].
         */
        private val lock = Any()

        /**
         * The prefix common to all KSP configurations of a project.
         */
        private const val configurationNamePrefix: String = "ksp"

        /**
         * The Maven coordinates of Google Auto Service annotations that
         * we [add][Project.addDependencies] as `compileOnly` dependencies to
         * the source sets of the project to which th
         */
        private const val autoServiceAnnotations: String =
            "com.google.auto.service:auto-service-annotations:1.1.1"
        /**
         * Contains projects to which [KspBasedPlugin]s already applied common settings.
         */
        private val commonSettingsApplied: MutableSet<Project> = mutableSetOf()
    }
}

private val Project.kspExtension: KspExtension?
    get() = extensions.findByType<KspExtension>()

private fun Project.useKsp2() {
    kspExtension?.apply {
        @OptIn(KspExperimental::class)
        useKsp2.set(true)
    }
}

/**
 * Makes KSP ignore sources under the `build/generated/source` directory
 * generated by `protoc`.
 */
private fun Project.excludeSourcesFromBuildDir() {
    kspExtension?.apply {
        excludedSources.from(generatedSourceProtoDir)
    }
}

/**
 * Adds `generated/<SourceSetName>/java`, `kotlin`, and `grpc` directories
 * to the Kotlin directory set for all source sets of this project.
 */
private fun Project.addProtoDataGeneratedSources() {
    sourceSets.configureEach {
        val ssn = SourceSetName(it.name)
        val sourceSetDir = generated(ssn)
        it.kotlinDirectorySet()?.srcDirs(
            sourceSetDir.resolve(java.name),
            sourceSetDir.resolve(kotlin.name),
            sourceSetDir.resolve(grpc.name)
        )
    }
}

//TODO:2025-03-16:alexander.yevsyukov: Take the extension from ProtoData.
private fun SourceSet.kotlinDirectorySet(): SourceDirectorySet? =
    extensions.findByName("kotlin") as SourceDirectorySet?

/**
 * Applies [KspGradlePlugin], if it is not yet added, to this project.
 */
private fun Project.applyKspPlugin() = with(KspGradlePlugin) {
    if (pluginManager.hasPlugin(id)) {
        return
    }
    pluginManager.apply(id)
}

/**
 * Makes `ksp<SourceSetName>Kotlin` tasks depend on corresponding
 * `launch<SourceSetName>ProtoData` tasks.
 *
 * This dependency is needed to avoid Gradle warning on disabled execution
 * optimization because of the absence of explicit or implicit dependencies.
 */
private fun Project.makeKspTasksDependOnProtoData() {
    afterEvaluate {
        val kspTasks = kspTasks()
        kspTasks.forEach { (ssn, kspTask) ->
            val protoDataTaskName = ProtoDataTaskName(ssn)
            val protoDataTask = tasks.findByName(protoDataTaskName.value())
            if (protoDataTask != null) {
                kspTask.dependsOn(protoDataTask)
            }
        }
    }
}

/**
 * The function replaces default destination directory defied by
 * [com.google.devtools.ksp.gradle.KspGradleSubplugin.getKspOutputDir] to
 * the one we used for all the generated code at the level of the project root.
 *
 * Also `kotlin` directory set for each source set gets new generated
 * Kotlin and Java source directories as its inputs.
 */
private fun Project.replaceKspOutputDirs() {
    afterEvaluate {
        val underBuild = KspGradlePlugin.defaultTargetDirectory(it).toString()
        val underProject = generatedDir.toString()
        kspTasks().forEach { (ssn, kspTask) ->
            kspTask.kspConfig.run {
                outputBaseDir.replace(underBuild, underProject)
                kotlinOutputDir.replace(underBuild, underProject)
                javaOutputDir.replace(underBuild, underProject)

                // KSP Gradle Plugin already added its output to source sets.
                // We need to add the replacement manually because we filtered
                // it before in `Project.excludeSourcesFromBuildDir()`.
                sourceSet(ssn).kotlinDirectorySet()?.run {
                    srcDirs(kotlinOutputDir)
                    srcDirs(javaOutputDir)
                }
            }
        }
    }
}

/**
 * Replaces the value of this property by setting a new path
 * where the [oldValue] is replaced with the [newValue].
 */
private fun Property<File>.replace(oldValue: String, newValue: String) {
    val current = get().path
    val replaced = current.replace(oldValue, newValue)
    set(File(replaced))
}
