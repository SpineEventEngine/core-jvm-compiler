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

package io.spine.tools.core.jvm.gradle

import io.spine.tools.meta.Module
import io.spine.tools.meta.ArtifactMeta
import io.spine.tools.meta.MavenArtifact

/**
 * Provides lazy loading of [ArtifactMeta] for a [Module].
 *
 * The metadata must be written into the resources of the module using
 * the Artifact Meta Gradle Plugin (ID: `io.spine.artifact-meta`).
 * Otherwise, accessing the [meta] property would result in an [IllegalStateException].
 *
 * @property module The module of the implementing class.
 * @property meta The lazily loaded metadata of the module to which the implementing class belongs.
 */
public abstract class LazyMeta(
    protected val module: Module,
) {
    /**
     * The meta-data of this artifact.
     */
    protected val meta: ArtifactMeta by lazy {
        ArtifactMeta.loadFromResource(module, this::class.java)
    }

    /**
     * Obtains the dependency of this artifact specified by the given [module]
     * as stored in the module [meta].
     *
     * The dependency must be previously written into the module resources using
     * the Artifact Meta (`io.spine.artifact-meta`) Gradle plugin.
     * @throws IllegalStateException if no dependency is found.
     */
    public fun dependency(module: Module): MavenArtifact {
        val found = meta.dependencies.find(module)
            ?: error("Unable to find the dependency `$module` in `$meta`.")
        return found as MavenArtifact
    }
}

/**
 * A dependency stored in [meta] for the given [module].
 *
 * @property meta The metadata of the [module] lazily loaded from resources.
 * @property module The module of the dependency.
 * @property artifact The artifact with full coordinates of the dependency.
 */
public data class LazyDependency(
    private val meta: LazyMeta,
    private val module: Module
) {
    public val artifact: MavenArtifact by lazy { meta.dependency(module) }
}
