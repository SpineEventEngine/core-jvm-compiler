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
import io.spine.tools.core.jvm.gradle.SPINE_TOOLS_GROUP
import io.spine.tools.core.jvm.gradle.plugins.ValidationSdk.javaCodegenBundle
import io.spine.tools.meta.LazyDependency
import io.spine.tools.meta.LazyMeta
import io.spine.tools.meta.MavenArtifact
import io.spine.tools.meta.Module

/**
 * Provides the dependencies of the CoreJvm Compiler.
 *
 * The class is public because its [artifact] property is used in
 * the integration tests module.
 *
 * See `artifactMeta/addDependencies` in `build.gradle.kts` of this module.
 */
public object Meta : LazyMeta(Module(SPINE_TOOLS_GROUP, "core-jvm-gradle-plugins")) {

    /**
     * The Maven artifact of the CoreJvm Compiler Gradle plugins.
     */
    @VisibleForTesting // See `CoreJvmPluginIgTest` under the `plugin-bundle` module.
    public val artifact: MavenArtifact
        get() = meta.artifact
}

/**
 * Artifacts of the Spine Validation SDK on which CoreJvm Compiler depends.
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
        Meta.dependency(javaCodegenBundle).withVersion(version)

    /**
     * The Maven artifact containing the `spine-validation-java-runtime` module.
     *
     * @param version The version of the Validation library to be used.
     *        If empty, the version of the build-time dependency is used.
     * @see javaCodegenBundle
     */
    @JvmStatic
    fun javaRuntime(version: String = ""): MavenArtifact =
        Meta.dependency(javaRuntime).withVersion(version)

    /**
     * The Maven artifact containing the `spine-validation-configuration` module.
     *
     * @param version The version of the Validation library to be used.
     *        If empty, the version of the build-time dependency is used.
     * @see javaCodegenBundle
     */
    @JvmStatic
    fun configuration(version: String = ""): MavenArtifact =
        Meta.dependency(configuration).withVersion(version)
}

/**
 * The dependency on Kotlin Gradle Plugin used when setting
 * the version of the plugin in integration tests.
 */
@VisibleForTesting
public object KotlinGradlePlugin {

    private val module = Module("org.jetbrains.kotlin", "kotlin-gradle-plugin")
    private val dependency: LazyDependency = LazyDependency(Meta, module)

    /**
     * The version of the plugin.
     */
    public val version: String = dependency.artifact.version
}

/**
 * The dependency on Protobuf Gradle Plugin used when setting
 * the version of the plugin in integration tests.
 */
public object ProtobufGradlePlugin {

    /**
     * The ID of the plugin.
     */
    public const val id: String = "com.google.protobuf"

    private val module = Module("com.google.protobuf", "protobuf-gradle-plugin")
    private val dependency: LazyDependency = LazyDependency(Meta, module)

    /**
     * The version of the plugin.
     */
    public val version: String = dependency.artifact.version

    public val coordinates: String
        get() = dependency.artifact.coordinates
}
