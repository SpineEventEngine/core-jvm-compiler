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

include(
    "annotator",
    "custom-annotations",
    "factories",
    "entity-queries",
    "known-types",
    "compiler",
    "rejection",
    "comparable",
    "validation-smoke"
)

/*
 * Dependency links established with the Gradle included build.
 *
 * See the `includeBuild(...)` block below for more info.
 */
val links = mapOf(
    "io.spine.tools:spine-core-jvm" to ":"
)

/*
 * Include the `mc-java` build into the `tests` project build.
 *
 * Integration tests are built separately in order to be able to test the current
 * version of the Gradle plugins.
 *
 * See the Gradle manual for more info:
 * https://docs.gradle.org/current/userguide/composite_builds.html
 */
includeBuild("$rootDir/../") {
    dependencySubstitution {
        links.forEach { (id, projectPath) ->
            substitute(module(id)).using(project(projectPath))
        }
    }
}
