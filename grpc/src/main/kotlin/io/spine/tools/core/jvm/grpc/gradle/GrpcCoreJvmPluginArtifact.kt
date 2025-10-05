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

package io.spine.tools.core.jvm.grpc.gradle

import io.spine.tools.core.jvm.gradle.DependencyHolder
import io.spine.tools.core.jvm.gradle.SPINE_TOOLS_GROUP
import io.spine.tools.meta.MavenArtifact
import io.spine.tools.meta.Module

/**
 * Provides dependencies of this module stored in resources by Artifact Meta Gradle plugin.
 */
internal object GrpcCoreJvmPluginArtifact : DependencyHolder(
    module = Module(SPINE_TOOLS_GROUP, "core-jvm-grpc")
)

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
    internal val artifact: MavenArtifact = GrpcCoreJvmPluginArtifact.dependency(module)
}
