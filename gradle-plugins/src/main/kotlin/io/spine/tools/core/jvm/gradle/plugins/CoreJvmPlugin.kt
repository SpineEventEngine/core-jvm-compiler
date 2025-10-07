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

import io.spine.string.simply
import io.spine.tools.core.jvm.VersionHolder
import io.spine.tools.core.jvm.gradle.CoreJvmOptions
import io.spine.tools.core.jvm.gradle.CoreJvmOptions.Companion.name
import io.spine.tools.core.jvm.gradle.coreJvmOptions
import io.spine.tools.core.jvm.grpc.gradle.GrpcCoreJvmPlugin
import io.spine.tools.core.jvm.routing.gradle.RoutingPlugin
import io.spine.tools.gradle.DslSpec
import io.spine.tools.gradle.lib.LibraryPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Spine Model Compiler for Java Gradle plugin.
 *
 * Applies all McJava sub-plugins to the given project.
 */
public class CoreJvmPlugin : LibraryPlugin<CoreJvmOptions>(
    DslSpec(name(), CoreJvmOptions::class)
) {

    public companion object {

        /**
         * The ID of the Gradle plugin.
         *
         * Make sure it is sync with the name of the file under `resources/META-INF/`.
         */
        public const val ID: String = "io.spine.core-jvm"
    }

    public override fun apply(project: Project) {
        super.apply(project)
        project.run {
            applyProtobufPlugin()
            pluginManager.withPlugin(ProtobufGradlePlugin.id) { _ ->
                applyCoreJvmPlugins()
            }
        }
    }
}

/**
 * Applies [ProtobufGradlePlugin] if it has not been applied yet.
 */
private fun Project.applyProtobufPlugin() {
    if (!pluginManager.hasPlugin(ProtobufGradlePlugin.id)) {
        // We carry the plugin as a runtime dependency of the fat JAR.
        // So it will be available in the build classpath and found by the ID.
        pluginManager.apply(ProtobufGradlePlugin.id)
    }
}

private fun Project.applyCoreJvmPlugins() {
    logApplying()
    val extension = project.coreJvmOptions
    extension.injectProject(project)
    createAndApplyPlugins()
}

private fun Project.logApplying() {
    val version = VersionHolder.version.value
    logger.warn("Applying `${simply<CoreJvmPlugin>()}` (version: $version) to `$name`.")
}

/**
 * Creates all the plugins that are parts of the CoreJvm Compiler and
 * applies them to this project.
 */
private fun Project.createAndApplyPlugins() {
    val plugins: List<Plugin<Project>> = listOf(
        CleaningPlugin(),
        GrpcCoreJvmPlugin(),
        CompilerConfigPlugin(),
        RoutingPlugin()
    )
    plugins.forEach {
        apply(it)
    }
}

private fun Project.apply(plugin: Plugin<Project>) {
    if (logger.isDebugEnabled) {
        logger.debug("Applying `${plugin.javaClass.name}` plugin.")
    }
    plugin.apply(project)
}
