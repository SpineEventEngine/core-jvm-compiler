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

@file:JvmName("Artifacts")

package io.spine.tools.core.jvm.gradle.plugins

import io.spine.annotation.VisibleForTesting
import io.spine.tools.meta.ArtifactMeta
import io.spine.tools.meta.MavenArtifact
import io.spine.tools.meta.Module
import io.spine.tools.core.jvm.gradle.SPINE_TOOLS_GROUP

/**
 * This file declares artifacts used and exposed by the CoreJvm Compiler.
 */
@Suppress("unused")
private const val ABOUT = ""

/**
 * Artifacts and dependencies of the CoreJvm Compiler.
 */
public object CoreJvmCompiler {

    /**
     * The Maven module of the CoreJvm Compiler which stores [meta].
     */
    private val pluginsModule = Module(SPINE_TOOLS_GROUP, "core-jvm-gradle-plugins")

    /**
     * The meta-data of the CoreJvm Compiler.
     */
    private val meta: ArtifactMeta by lazy {
        ArtifactMeta.loadFromResource(pluginsModule, this::class.java)
    }

    /**
     * The Maven artifact of the CoreJvm Compiler Gradle plugins.
     */
    @VisibleForTesting // See `CoreJvmPluginIgTest` under the `plugin-bundle` module.
    public val artifact: MavenArtifact
        get() = meta.artifact

    /**
     * Obtains the dependency of the CoreJvm Compiler specified by the given [module]
     * as stored in the module [meta].
     *
     * @throws IllegalStateException if no dependency is found.
     */
    internal fun dependency(module: Module): MavenArtifact {
        val found = meta.dependencies.find(module)
            ?: error("Unable to find the dependency `$module` in `$meta`.")
        return found as MavenArtifact
    }
}

/**
 * The gRPC plugin to `protoc` which CoreJvm Compiler passes to
 * Protobuf Gradle Plugin when the Compiler's Gradle plugin is applied.
 *
 * See `artifactMeta/addDependencies` in `build.gradle.kts` of this module.
 *
 * @see io.spine.tools.core.jvm.gradle.plugins.EnableGrpcPlugin
 */
internal object GrpcProtocPlugin {

    /**
     * The module of the `protoc` plugin of gRPC for Java.
     */
    private val module = Module("io.grpc", "protoc-gen-grpc-java")

    /**
     * The artifact of the gRPC `protoc` plugin for Java.
     */
    internal val artifact: MavenArtifact = CoreJvmCompiler.dependency(module)
}

/**
 * Artifacts of the Spine Validation SDK on which [CoreJvmCompiler] depends.
 *
 * See `artifactMeta/addDependencies` in `build.gradle.kts` of this module.
 */
@Suppress("ConstPropertyName")
internal object ValidationSdk {

    private const val group = "io.spine.validation"
    private const val prefix = "spine-validation"
    private val javaCodegenBundle = Module(group, "$prefix-java-bundle")
    private val javaRuntime = Module(group, "$prefix-java-runtime")
    private val configuration = Module(group, "$prefix-configuration")

    private fun MavenArtifact.withVersion(version: String): MavenArtifact {
        version.ifEmpty {
            return this
        }
        return MavenArtifact(group, name, version, classifier, extension)
    }

    /**
     * The Maven artifact containing the `spine-validation-java-bundle` module.
     *
     * @param version The version of the Validation library to be used.
     *        If empty, the version of the build-time dependency is used.
     */
    @JvmStatic
    fun javaCodegenBundle(version: String = ""): MavenArtifact =
        CoreJvmCompiler.dependency(javaCodegenBundle).withVersion(version)

    /**
     * The Maven artifact containing the `spine-validation-java-runtime` module.
     *
     * @param version The version of the Validation library to be used.
     *        If empty, the version of the build-time dependency is used.
     * @see javaCodegenBundle
     */
    @JvmStatic
    fun javaRuntime(version: String = ""): MavenArtifact =
        CoreJvmCompiler.dependency(javaRuntime).withVersion(version)

    /**
     * The Maven artifact containing the `spine-validation-configuration` module.
     *
     * @param version The version of the Validation library to be used.
     *        If empty, the version of the build-time dependency is used.
     * @see javaCodegenBundle
     */
    @JvmStatic
    fun configuration(version: String = ""): MavenArtifact =
        CoreJvmCompiler.dependency(configuration).withVersion(version)
}
