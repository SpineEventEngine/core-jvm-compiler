/*
 * Copyright 2026 CodeMatters, Lda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package io.spine.tools.core.jvm.grpc.gradle

import io.spine.tools.protobuf.gradle.protobufExtension
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
            // Configure the tasks as Gradle creates them, instead of creating them here.
            // Protobuf Gradle Plugin registers its own action finishing the configuration
            // of the tasks after running this block, so this action precedes it.
            protobuf.generateProtoTasks.all().configureEach { t ->
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
