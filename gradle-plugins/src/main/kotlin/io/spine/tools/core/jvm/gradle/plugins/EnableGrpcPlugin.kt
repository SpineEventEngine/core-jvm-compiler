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

import com.google.protobuf.gradle.ExecutableLocator
import com.google.protobuf.gradle.GenerateProtoTask
import io.spine.tools.core.jvm.gradle.gRpcProtocPlugin
import io.spine.tools.gradle.ProtocConfigurationPlugin
import io.spine.tools.gradle.ProtocPluginName.grpc
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

/**
 * A Gradle plugin that enablers gRPC in a project.
 */
public class EnableGrpcPlugin : ProtocConfigurationPlugin() {

    override fun configureProtocPlugins(
        plugins: NamedDomainObjectContainer<ExecutableLocator>,
        project: Project
    ) {
        plugins.create(grpc.name) { locator ->
            locator.artifact = gRpcProtocPlugin.notation()
        }
    }

    override fun customizeTask(protocTask: GenerateProtoTask) {
        protocTask.plugins.create(grpc.name)
    }
}
