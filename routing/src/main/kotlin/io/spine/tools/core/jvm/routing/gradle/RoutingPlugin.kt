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

package io.spine.tools.core.jvm.routing.gradle

import io.spine.tools.core.jvm.ksp.gradle.KspBasedPlugin
import io.spine.tools.gradle.Artifact
import io.spine.tools.gradle.Artifact.SPINE_TOOLS_GROUP
import io.spine.tools.gradle.artifact
import io.spine.tools.meta.ArtifactMeta
import io.spine.tools.meta.Module

/**
 * Applies this [thisModule] as a plugin to KSP by calculating [mavenCoordinates].
 */
public class RoutingPlugin : KspBasedPlugin() {

    override val mavenCoordinates: String
        get() = routingKspPlugin.notation()

    private val routingVersion: String by lazy {
        val meta = ArtifactMeta.loadFromResource(thisModule, this::class.java)
        meta.version
    }

    private val routingKspPlugin: Artifact by lazy {
        artifact {
            useSpineToolsGroup()
            setName(thisModule.name)
            setVersion(routingVersion)
        }
    }

    private companion object {

        /**
         * The Maven module to which this class belongs.
         */
        private val thisModule = Module(SPINE_TOOLS_GROUP, "core-jvm-routing")
    }
}
