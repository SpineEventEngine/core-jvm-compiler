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

@file:JvmName("StubResolution")

package io.spine.tools.core.jvm

import java.io.File
import org.gradle.api.Project

/**
 * The name of the system property holding the path to the local Maven
 * repository with the third-party artifacts stub projects resolve.
 *
 * The repository is prepared by the `prepareStubRepo` task of the
 * test-hosting module, which copies the artifacts from the dependency
 * cache of the enclosing build.
 */
const val STUB_REPOSITORY_PROPERTY: String = "stub.repository"

/**
 * The local Maven repository with the third-party artifacts stub projects
 * resolve, or `null` if the test-hosting module does not prepare one.
 *
 * @see STUB_REPOSITORY_PROPERTY
 */
val stubRepository: File?
    get() = System.getProperty(STUB_REPOSITORY_PROPERTY)
        ?.takeIf { it.isNotBlank() }
        ?.let(::File)

/**
 * Forbids this stub project to resolve dependencies via the network.
 *
 * A project created via [org.gradle.testfixtures.ProjectBuilder] cannot
 * reuse the dependency cache of the machine: `ProjectBuilder` replaces
 * the persistent caches with empty in-memory ones (see
 * `TestGlobalScopeServices.createCacheFactory` in Gradle). Every resolution
 * performed by such a project would therefore fetch the whole dependency
 * graph from remote repositories anew on each test run. Under the
 * consumption limits of Maven Central, such traffic gets the whole machine
 * blocked with HTTP 429 responses.
 *
 * Stub projects must instead resolve from local repositories only:
 * `mavenLocal()` and [the stub repository][stubRepository] prepared by
 * the enclosing build.
 *
 * A resolution failing with "No cached version available for offline mode"
 * signals that a stub project requires an artifact these repositories do
 * not serve; add the artifact to the `stubRepoDeps` configuration of
 * the test-hosting module.
 *
 * Call right after the project is built, before any dependency resolution.
 *
 * @see <a href="https://central.sonatype.org/faq/429-error/">Maven Central
 *   consumption limits</a>
 */
fun Project.forbidNetworkResolution() {
    gradle.startParameter.isOffline = true
}
