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

import io.spine.tools.gradle.protobuf.protobufExtension
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.provider.Property

/**
 * Allows configuring the usage of gRPC in a Spine-based project.
 *
 * @see GrpcCoreJvmPlugin
 */
public abstract class GrpcSettings @Inject public constructor(
    private val project: Project
) {
    /**
     * Tells if the project to which the [GrpcCoreJvmPlugin] is applied
     * is going to have generated gRPC code.
     */
    public abstract val enabled: Property<Boolean>

    init {
        enabled.convention(false)
        configureEnablingGrpc()
    }

    private fun configureEnablingGrpc() {
        val protobuf = project.protobufExtension!!
        protobuf.generateProtoTasks { // This delays the execution until `afterEvaluate`.
            if (!enabled.get()) {
                // gRPC was not enabled in the project.
                return@generateProtoTasks
            }
            protobuf.plugins { plugins ->
                plugins.create(PROTOC_PLUGIN_JAVA_ID) { locator ->
                    locator.artifact = GrpcKotlin.javaProtocPlugin.artifact.coordinates
                }
                plugins.create(PROTOC_PLUGIN_KOTLIN_ID) { locator ->
                    locator.artifact = GrpcKotlin.kotlinProtocPlugin.artifact.coordinates
                }
            }
            val protocTasks = protobuf.generateProtoTasks.all()
            protocTasks.forEach { t ->
                t.plugins.create(PROTOC_PLUGIN_JAVA_ID)
                t.plugins.create(PROTOC_PLUGIN_KOTLIN_ID)
            }
            // Add dependencies in the scope of `generateProtoTasks` so that we add it
            // only when the project is evaluated, and we know the value of the `enabled` property.
            project.dependencies.add("implementation", GrpcKotlin.stubLibrary.artifact.coordinates)
        }
    }

    internal companion object {

        /**
         * The name of the DSL element under `coreJvm`.
         */
        internal const val NAME = "grpc"

        /**
         * The name of the `protoc` gRPC plugin for Java.
         */
        private const val PROTOC_PLUGIN_JAVA_ID = "grpc"

        /**
         * The name of the `protoc` gRPC plugin for Kotlin.
         */
        private const val PROTOC_PLUGIN_KOTLIN_ID = "grpckt"
    }
}
