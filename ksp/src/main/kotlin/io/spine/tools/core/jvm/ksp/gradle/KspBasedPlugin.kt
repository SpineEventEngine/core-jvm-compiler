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

package io.spine.tools.core.jvm.ksp.gradle

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.gradle.KspExtension
import com.google.protobuf.gradle.ProtobufExtension
import io.spine.tools.compiler.gradle.api.CompilerTaskName
import io.spine.tools.compiler.gradle.api.generatedDir
import io.spine.tools.core.jvm.ksp.gradle.KspBasedPlugin.Companion.autoServiceKsp
import io.spine.tools.core.jvm.ksp.gradle.KspBasedPlugin.Companion.commonSettingsApplied
import io.spine.tools.fs.DirectoryName.grpc
import io.spine.tools.fs.DirectoryName.java
import io.spine.tools.fs.DirectoryName.kotlin
import io.spine.tools.gradle.project.findKotlinCompileFor
import io.spine.tools.gradle.project.sourceSet
import io.spine.tools.gradle.project.sourceSets
import io.spine.tools.gradle.task.findKotlinDirectorySet
import java.io.File
import kotlin.io.path.pathString
import ksp.com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
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
 *  2. Makes a KSP task depend on a `LaunchSpineCompiler` task for the same source set.
 *
 *  3. Adds the artifact specified by the [mavenCoordinates] property, and [autoServiceKsp]
 *   as the dependencies of the KSP configurations of the project.
 */
public abstract class KspBasedPlugin : Plugin<Project> {

    /**
     * The Maven coordinates of the plugin to be added to KSP configurations
     * in the project to which the plugin is applied.
     */
    protected abstract val mavenCoordinates: String

    @OverridingMethodsMustInvokeSuper
    override fun apply(project: Project) {
        project.run {
            pluginManager.withPlugin(KspGradlePlugin.id) {
                applyCommonSettings()
                addPluginsToKspConfigurations()
            }
            // If the KSP plugin is already applied, the above code would be executed.
            // Otherwise, we apply the plugin by ourselves, which would run the above code.
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
                makeKspIgnoreProtocOutputDir()
                addSourcesGeneratedBySpineCompiler()
                makeKspTasksDependOnSpineCompiler()
                makeCompileKotlinTasksDependOnKspTasks()
                replaceKspOutputDirs()
                commonSettingsApplied.add(this)
            }
        }
    }

    private fun Project.addPluginsToKspConfigurations() {
        configurations
            .filter { it.name.startsWith(configurationNamePrefix) }
            .forEach { kspConfiguration ->
                val configName = kspConfiguration.name
                project.dependencies.run {
                    add(configName, mavenCoordinates)
                    add(configName, autoServiceKsp)
                }
            }
    }

    private fun Project.addDependencies() {
        sourceSets.forEach { sourceSet ->
            val configurationName = sourceSet.compileOnlyConfigurationName
            dependencies.add(
                configurationName,
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
         * The Maven coordinates for the Auto Service processor for Kotlin.
         */
        private const val autoServiceKsp: String =
            "dev.zacsweers.autoservice:auto-service-ksp:1.2.0"

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
 * Obtains the root directory into which Protobuf Gradle Plugin assigns the `protoc` output.
 */
private val Project.protocOutputDir: File?
    get() {
        val protobufExtension = extensions.findByType(ProtobufExtension::class.java)
        val path = protobufExtension?.generatedFilesBaseDir
        return path?.let { File(it) }
    }

/**
 * Makes KSP ignore sources under the `build/generated/sources` directory
 * generated by `protoc` to avoid the duplicated types with the code
 * produced by the Compiler.
 */
private fun Project.makeKspIgnoreProtocOutputDir() {
    kspExtension?.apply {
        protocOutputDir?.let {
            excludedSources.from(it)
        }
    }
}

/**
 * Adds `generated/<SourceSetName>/java`, `kotlin`, and `grpc` directories
 * to the Kotlin directory set for all source sets of this project.
 */
private fun Project.addSourcesGeneratedBySpineCompiler() {
    sourceSets.configureEach {
        val sourceSetDir = generatedDir(it, "")
        it.findKotlinDirectorySet()?.srcDirs(
            sourceSetDir.resolve(java.name),
            sourceSetDir.resolve(kotlin.name),
            sourceSetDir.resolve(grpc.name)
        )
    }
}

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
 * `launch<SourceSetName>SpineCompiler` tasks.
 *
 * This dependency is needed to avoid Gradle warning on disabled execution
 * optimization because of the absence of explicit or implicit dependencies.
 */
private fun Project.makeKspTasksDependOnSpineCompiler() {
    afterEvaluate {
        val kspTasks = kspTasks()
        kspTasks.forEach { (ssn, kspTask) ->
            val taskName = CompilerTaskName(ssn)
            // Even if a task with `taskName` does not exist, the call
            // to `mustRunAfter` won't fail.
            // We do this instead of `dependsOn` because historically it
            // proves to be unreliable in this particular case.
            kspTask.mustRunAfter(taskName.value())
            logger.warn(
                "[CoreJvm Compiler]" +
                        " `${kspTask.name}` set to run after" +
                        " `${taskName.value()}` in the project `$name`."
            )
        }
    }
}

/**
 * Makes `compile<SourceSetName>Kotlin` tasks depend on `ksp<SourceSetName>Kotlin` tasks.
 *
 * Strangely, at the time of writing, this is not arranged by KSP Gradle Plugin.
 * So, we set this dependency to avoid the Gradle error when McJava is applied.
 */
private fun Project.makeCompileKotlinTasksDependOnKspTasks() {
    afterEvaluate {
        val kspTasks = kspTasks()
        kspTasks.forEach { (ssn, kspTask) ->
            val sourceSet = sourceSets.findByName(ssn.value)
            val compileKotlin = findKotlinCompileFor(sourceSet!!)
            compileKotlin?.dependsOn(kspTask)
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
                outputBaseDir.replacePath(underBuild, underProject)
                kotlinOutputDir.replacePath(underBuild, underProject)
                javaOutputDir.replacePath(underBuild, underProject)
                resourceOutputDir.replacePath(underBuild, underProject)

                val sourceSet = sourceSet(ssn)
                sourceSet.run {
                    // KSP Gradle Plugin already added its output to source sets.
                    // We need to add the replacement manually because we filtered
                    // it before in `Project.makeKspIgnoreGeneratedSourceProtoDir()`.

                    java.srcDirs(javaOutputDir)
                    findKotlinDirectorySet()?.srcDirs(kotlinOutputDir)

                    // We need to add redirected KSP-generated resources too.
                    resources.srcDirs(resourceOutputDir)
                }
            }
        }
    }
}

/**
 * Replaces the value of this property by setting a new path
 * where the [oldValue] is replaced with the [newValue].
 */
@Suppress("unused") // See docs for `Project.replaceKspOutputDirs()`.
context(project: Project)
private fun DirectoryProperty.replacePath(oldValue: String, newValue: String) {
    val currentDir = get().asFile.toPath()
    val replacedPath = currentDir.pathString.replace(oldValue, newValue)
    set(project.layout.projectDirectory.dir(replacedPath))
}
