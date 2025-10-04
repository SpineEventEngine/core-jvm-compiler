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
 * The DSL syntax looks like this:
 *
 * ```kotlin
 * spine {
 *     coreJvm {
 *        grpc {
 *           enabled.set(true)
 *        }
 *     }
 * }
 * ```
 * The default value is `false`.
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
    public var enabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    init {
        enabled.convention(false)
        project.afterEvaluate {
            turnGrpc(enabled.get())
        }
    }

    private fun turnGrpc(value: Boolean) {
        val protobuf = project.protobufExtension!!
        protobuf.plugins { plugins ->
            if (value) {
                plugins.create(PROTOC_PLUGIN_ID) {
                    it.artifact = GrpcProtocPlugin.artifact.coordinates
                }
            }
        }
    }

    internal companion object {

        /**
         * The name of the DSL element under `coreJvm`.
         */
        internal const val NAME = "grpc"

        /**
         * The name of the gRPC plugin to `protoc` for Java.
         */
        private const val PROTOC_PLUGIN_ID = "grpc"
    }
}
