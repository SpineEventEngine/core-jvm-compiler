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

package io.spine.tools.mc.java.gradle.plugins

import io.spine.string.simply
import io.spine.tools.gradle.Artifact
import io.spine.tools.gradle.DependencyVersions
import io.spine.tools.gradle.protobuf.ProtobufDependencies.gradlePlugin
import io.spine.tools.gradle.protobuf.ProtobufDependencies.protobufCompiler
import io.spine.tools.gradle.protobuf.protobufExtension
import io.spine.tools.mc.gradle.LanguagePlugin
import io.spine.tools.mc.java.VersionHolder
import io.spine.tools.mc.java.gradle.McJavaOptions
import io.spine.tools.mc.java.gradle.McJavaOptions.Companion.name
import io.spine.tools.mc.java.gradle.mcJava
import io.spine.tools.mc.java.routing.gradle.RoutingPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Spine Model Compiler for Java Gradle plugin.
 *
 * Applies all McJava sub-plugins to the given project.
 */
public class McJavaPlugin : LanguagePlugin(name(), McJavaOptions::class.java.kotlin) {

    public override fun apply(project: Project) {
        super.apply(project)
        project.pluginManager.withPlugin(gradlePlugin.id) { _ ->
            project.applyMcJava()
        }
    }
}

private fun Project.applyMcJava() {
    logApplying()
    setProtocArtifact()
    val extension = project.mcJava
    extension.injectProject(project)
    createAndApplyPlugins()
}

private fun Project.logApplying() {
    val version = VersionHolder.version.value
    logger.warn("Applying `${simply<McJavaPlugin>()}` (version: $version) to `$name`.")
}

private fun Project.setProtocArtifact() {
    val ofPluginBase = DependencyVersions.loadFor(Artifact.PLUGIN_BASE_ID)
    val protocArtifact = protobufCompiler.withVersionFrom(ofPluginBase).notation()
    protobufExtension!!.protoc { locator ->
        locator.artifact = protocArtifact
    }
}

/**
 * Creates all the plugins that are parts of `mc-java` and applies them to this project.
 */
private fun Project.createAndApplyPlugins() {
    val plugins: List<Plugin<Project>> = listOf(
        CleaningPlugin(),
        EnableGrpcPlugin(),
        ProtoDataConfigPlugin(),
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
