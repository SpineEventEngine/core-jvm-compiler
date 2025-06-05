/*
 * Copyright 2022, TeamDev. All rights reserved.
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

@file:JvmName("StandardRepos")

package io.spine.tools.mc.java

import java.net.URI
import org.gradle.api.artifacts.dsl.RepositoryHandler

/**
 * Adds the standard Maven repositories to the receiver [RepositoryHandler].
 *
 * This is analogous to the eponymous method in the build scripts with the exception that this
 * method is available at the module's test runtime.
 *
 * Note that not all the Maven repositories may be added to the test projects, but only those that
 * are required for tests. We are not trying to keep these repositories is perfect synchrony with
 * the ones defined in build scripts.
 */
fun RepositoryHandler.applyStandard() {
    mavenLocal()
    mavenCentral()
    val registryBaseUrl = "https://europe-maven.pkg.dev/spine-event-engine"
    maven {
        it.url = URI("$registryBaseUrl/releases")
    }
    maven {
        it.url = URI("$registryBaseUrl/snapshots")
    }
}
