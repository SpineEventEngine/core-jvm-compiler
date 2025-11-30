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

import io.spine.tools.core.jvm.ksp.gradle.Meta.autoServiceKspProcessor
import ksp.com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Configures a Gradle project to run [KSP](https://kotlinlang.org/docs/ksp-overview.html) with
 * a processor specified by the [mavenCoordinates] property.
 *
 * The plugin performs the following configuration steps:
 *
 * 1. Adds the [KSP Gradle Plugin](https://github.com/google/ksp) to the project
 *   if it is not added already.
 *
 * 2. Makes a KSP task depend on a `LaunchSpineCompiler` task for the same source set.
 *
 * 3. Adds the artifact specified by the [mavenCoordinates] property,
 *   and `AutoServiceKsp.processor` as the dependencies of the KSP configurations of the project.
 */
public abstract class KspBasedPlugin : Plugin<Project> {

    /**
     * The Maven coordinates of the plugin to be added to KSP configurations
     * in the project to which the plugin is applied.
     */
    protected abstract val mavenCoordinates: String

    @OverridingMethodsMustInvokeSuper
    override fun apply(project: Project) {
        project.pluginManager.apply(CommonKspSettingsPlugin::class.java)
        project.addPluginsToKspConfigurations()
    }

    private fun Project.addPluginsToKspConfigurations() {
        configurations
            .filter { it.name.startsWith(configurationNamePrefix) }
            .forEach { kspConfiguration ->
                val configName = kspConfiguration.name
                dependencies.run {
                    add(configName, mavenCoordinates)
                    add(configName, autoServiceKspProcessor.artifact.coordinates)
                }
            }
    }

    @Suppress("ConstPropertyName")
    protected companion object {

        /**
         * The prefix common to all KSP configurations of a project.
         */
        private const val configurationNamePrefix: String = "ksp"
    }
}
