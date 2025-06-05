/*
 * Copyright 2023, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.tools.mc.java.gradle

import io.spine.tools.gradle.Artifact
import io.spine.tools.gradle.Artifact.SPINE_TOOLS_GROUP
import io.spine.tools.gradle.Dependency
import io.spine.tools.gradle.DependencyVersions
import io.spine.tools.gradle.ThirdPartyDependency
import io.spine.tools.gradle.artifact

/**
 * This file declares artifacts used and exposed by McJava.
 */
@Suppress("unused")
private const val ABOUT = ""

private const val JAR_EXTENSION = "jar"
private const val GRPC_GROUP = "io.grpc"
private const val GRPC_PLUGIN_NAME = "protoc-gen-grpc-java"
private const val MC_JAVA_NAME = "spine-gradle-plugins"
private const val ALL_CLASSIFIER = "all"

/**
 * The name of the Maven artifact containing both Spine Protobuf compiler plugin
 * and `modelCompiler` plugin.
 */
internal const val SPINE_MC_JAVA_ALL_PLUGINS_NAME = "spine-core-jvm-plugins"

/**
 * Versions of dependencies used by McJava.
 */
private val versions = DependencyVersions.loadFor(MC_JAVA_NAME)

/**
 * The type alias to avoid the confusion with the "third-party" part of
 * the class name used for the default implementation of the [Dependency] interface.
 */
internal typealias MavenDependency = ThirdPartyDependency

/**
 * The Maven artifact of the gRPC Protobuf compiler plugin.
 */
@get:JvmName("gRpcProtocPlugin")
internal val gRpcProtocPlugin: Artifact by lazy {
    val gRpcPlugin: Dependency = MavenDependency(GRPC_GROUP, GRPC_PLUGIN_NAME)
    gRpcPlugin.withVersionFrom(versions)
}

/**
 * Artifacts of McJava.
 */
internal object McJava {

    /**
     * The version of the Model Compiler Java modules.
     *
     * This is the version of all the modules declared in this project.
     */
    @JvmStatic
    @get:JvmName("version")
    internal val version: String by lazy {
        val self: Dependency = MavenDependency(SPINE_TOOLS_GROUP, MC_JAVA_NAME)
        versions.versionOf(self)
            .orElseThrow { error("Unable to load versions of `$self`.") }
    }

    /**
     * The Maven artifact containing the `spine-mc-java-plugins:all` fat JAR artifact.
     */
    @JvmStatic
    @get:JvmName("allPlugins")
    internal val allPlugins: Artifact by lazy {
        artifact {
            useSpineToolsGroup()
            setName(SPINE_MC_JAVA_ALL_PLUGINS_NAME)
            setVersion(this@McJava.version)
            setClassifier(ALL_CLASSIFIER)
            setExtension(JAR_EXTENSION)
        }
    }
}

/**
 * Artifacts of the Spine Validation SDK.
 */
internal object ValidationSdk {

    @Suppress("ConstPropertyName")
    private const val group = "io.spine.validation"
    private val javaCodegen = MavenDependency(group, "spine-validation-java")
    private val javaCodegenBundle = MavenDependency(group, "spine-validation-java-bundle")
    private val javaRuntime = MavenDependency(group, "spine-validation-java-runtime")
    private val configuration = MavenDependency(group, "spine-validation-configuration")

    private fun validationVersion(version: String = ""): String =
        version.ifEmpty {
            versions.versionOf(javaCodegen).orElseThrow {
                error("Unable to load the version of `$javaCodegen`.")
            }
        }

    /**
     * The Maven artifact containing the `spine-validation-java-bundle` module.
     *
     * @param version
     *         the version of Validation library to be used.
     *         If empty, the version of the build time dependency used is used.
     * @see javaRuntime
     */
    @JvmStatic
    fun javaCodegenBundle(version: String = ""): Artifact = artifact {
        dependency = javaCodegenBundle
        setVersion(validationVersion(version))
    }

    /**
     * The Maven artifact containing the `spine-validation-java-runtime` module.
     *
     * @param version
     *         the version of the Validation library to be used.
     *         If empty, the version of the build time dependency used is used.
     * @see javaCodegenBundle
     */
    @JvmStatic
    fun javaRuntime(version: String = ""): Artifact = artifact {
        dependency = javaRuntime
        setVersion(validationVersion(version))
    }

    /**
     * The Maven artifact containing the `spine-validation-configuration` module.
     *
     * @param version
     *         the version of the Validation library to be used.
     *         If empty, the version of the build time dependency used is used.
     * @see javaCodegenBundle
     */
    @JvmStatic
    fun configuration(version: String = ""): Artifact = artifact {
        dependency = configuration
        setVersion(validationVersion(version))
    }
}
