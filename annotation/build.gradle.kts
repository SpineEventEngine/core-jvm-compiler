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

import io.spine.dependency.local.Base
import io.spine.dependency.local.Logging
import io.spine.dependency.local.TestLib
import io.spine.dependency.local.ToolBase
import io.spine.gradle.report.coverage.creditTestCoverageFrom

plugins {
    module
    id("io.spine.core-jvm")
}

dependencies {
    implementation(project(":base"))
    implementation(Logging.lib)

    arrayOf(Base.annotations, Base.lib).forEach {
        testFixturesImplementation(it)?.because(
            "Test fixtures use `io.spine.annotation.Internal` and `io.spine.code.proto`," +
                    " which used to arrive transitively with the retired `tool-base` artifact."
        )
    }
    testFixturesImplementation(TestLib.lib)
    testFixturesApi(ToolBase.psiJava)?.because(
        "Test fixtures check the generated code parsed with PSI."
    )

    testImplementation(ToolBase.fs)
    testImplementation(ToolBase.pluginTestlib)
    testImplementation(gradleTestKit())
}

forceBaseInProtoTasks()

/**
 * The renderers and views of this module are exercised end-to-end by
 * the `annotation-tests` module, which runs the Compiler pipeline in-process.
 * Credit that coverage to this module's own Kover report, which otherwise
 * sees only this module's `test` data.
 */
creditTestCoverageFrom(project(":annotation-tests"))

/**
 * Tests use the artifacts published to `mavenLocal`, so we need to publish them all first.
 */
tasks.test {
    dependsOn(rootProject.tasks.named("localPublish"))
}

tasks.withType<ProcessResources>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

afterEvaluate {
    spineCompilerRemoteDebug(enabled = false)
}
