/*
 * Copyright 2026, TeamDev. All rights reserved.
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

package io.spine.dependency.local

/**
 * Dependencies on the CoreJvm Compiler artifacts.
 *
 * See [CoreJvm Compiler](https://github.com/SpineEventEngine/core-jvm-compiler).
 */
@Suppress(
    "MemberVisibilityCanBePrivate" /* `pluginLib()` is used by subprojects. */,
    "ConstPropertyName",
    "unused"
)
object CoreJvmCompiler {

    /**
     * The Compiler belongs to the `tools` group.
     */
    const val group = Spine.toolsGroup

    /**
     * The version used in the build classpath.
     */
    const val dogfoodingVersion = "2.0.0-SNAPSHOT.082"

    /**
     * The version to be used for integration tests.
     */
    const val version = "2.0.0-SNAPSHOT.082"

    /**
     * The ID of the Gradle plugin.
     */
    const val pluginId = "io.spine.core-jvm"

    /**
     * The library carrying the CoreJvm Gradle Plugin with the [dogfoodingVersion].
     *
     * The [dogfoodingVersion] predates the split of the `plugins` module, so
     * the plugin classes still come with the fat JAR artifact. Once the
     * [dogfoodingVersion] is bumped to a version published after the split,
     * switch this property to [gradlePluginLib].
     */
    val pluginLib = fatJarLib(dogfoodingVersion)

    /**
     * The name of the published fat JAR artifact with the Spine Compiler plugins.
     *
     * The artifact is produced by the `compiler-plugins` module.
     */
    const val fatJarArtifact = "core-jvm-plugins"

    /**
     * The name of the published artifact with the CoreJvm Gradle Plugin.
     *
     * The artifact is produced by the `gradle-plugin` module.
     * Its POM declares a runtime dependency on [the fat JAR][fatJarArtifact].
     */
    const val gradlePluginArtifact = "core-jvm-gradle-plugin"

    /**
     * The fat JAR library with the given [version].
     */
    fun fatJarLib(version: String): String = "$group:$fatJarArtifact:$version"

    /**
     * The CoreJvm Gradle Plugin library with the given [version].
     *
     * Only versions published after the split of the `plugins` module
     * provide this artifact.
     */
    fun gradlePluginLib(version: String): String = "$group:$gradlePluginArtifact:$version"

    /**
     * The artifact reference for forcing in configurations.
     */
    val pluginsArtifact: String = fatJarLib(version)
}
