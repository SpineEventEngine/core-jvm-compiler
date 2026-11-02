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

import io.spine.dependency.lib.KotlinPoet
import io.spine.dependency.lib.Roaster
import io.spine.dependency.local.Base
import io.spine.dependency.local.Compiler
import io.spine.dependency.local.CoreJvm
import io.spine.dependency.local.Logging
import io.spine.dependency.local.TestLib
import io.spine.dependency.local.ToolBase
import io.spine.dependency.local.Validation
import io.spine.gradle.report.coverage.creditTestCoverageFrom

plugins {
    module
    id("io.spine.artifact-meta")
    id("io.spine.core-jvm")
}

/**
 * The ID used for publishing this module.
 */
val moduleArtifactId = "core-jvm-base"

artifactMeta {
    artifactId.set(moduleArtifactId)
    addDependencies(
        Compiler.pluginLib,
        CoreJvm.server,
        CoreJvm.client,
    )
    excludeConfigurations {
        containing(*buildToolConfigurations)
    }
}

dependencies {
    compileOnlyApi(gradleApi())
    compileOnlyApi(gradleKotlinDsl())

    val apiDeps = arrayOf(
        Compiler.api,
        Compiler.jvm,
        Validation.context,
        ToolBase.protoCode,
        ToolBase.pluginBase,
        ToolBase.protobufSetupPlugins,
        KotlinPoet.lib,
    )
    apiDeps.forEach {
        api(it) {
            excludeSpineBase()
        }
    }
    api(ToolBase.classicCodegen) {
        excludeSpineBase()
        // `classic-codegen` still brings Roaster, which nothing here uses.
        // Drop the exclusion when ToolBase removes Roaster from `classic-codegen`.
        exclude(group = Roaster.group)
    }
    api(Base.lib)

    arrayOf(
        Compiler.gradleApi,
        Logging.lib,
        ToolBase.jvmTools,
        ToolBase.gradlePluginApi
    ).forEach {
        implementation(it)
    }

    arrayOf(
        Base.lib,
        gradleTestKit() /* for creating a Gradle project. */,
        TestLib.lib,
        Compiler.testlib /* `PipelineSetup` API. */
    ).forEach {
        // Expose using API level for the submodules.
        testFixturesApi(it)
    }
    testFixturesImplementation(Validation.java)?.because(
        "We apply `JavaValidationPlugin` before a plugin under a test in `PluginTestSetup`."
    )

    testImplementation(TestLib.lib)
    testImplementation(gradleTestKit())
    testImplementation(gradleKotlinDsl())?.because(
        "Production code uses Gradle Kotlin DSL extensions (e.g. in `ModuleOptions`)," +
                " which are `compileOnly` and thus needed on the test runtime classpath."
    )
    testImplementation(ToolBase.pluginTestlib)
    testFixturesImplementation(Logging.testLib)?.because(
        "Because we need `tapConsole`."
    )
}

forceSpineBase()

/**
 * Add coverage of the `AddFieldClass` from the integration tests executed the referenced project.
 */
creditTestCoverageFrom(project(":entity-tests"))
