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
 * This build is deliberately NOT a Gradle composite build of `core-jvm-compiler`.
 *
 * It consumes the artifacts that the root build publishes to Maven Local with its
 * `localPublish` task, which the root `integrationTests` task runs before this build.
 * See `build.gradle.kts` for the repository declarations that make Maven Local win
 * for those artifacts.
 *
 * Do not add `includeBuild("$rootDir/../")` here. Gradle would then substitute the
 * `core-jvm-*` artifacts with the projects of the root build and run their code
 * generation, compilation, and packaging tasks from here, overwriting the outputs in
 * the root project directories. Since `integrationTests` runs after the `test` tasks
 * of the root build and before its coverage report, the report would then analyze
 * class files other than those the tests ran against, and randomly drop the coverage
 * of the mismatched classes.
 */
