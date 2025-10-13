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

import io.spine.dependency.lib.KotlinPoet
import io.spine.dependency.local.Base
import io.spine.dependency.local.Compiler
import io.spine.dependency.local.CoreJvm
import io.spine.dependency.local.Logging
import io.spine.dependency.local.TestLib
import io.spine.dependency.local.ToolBase
import io.spine.dependency.local.Validation

plugins {
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
        Validation.configuration,
        ToolBase.classicCodegen,
        ToolBase.pluginBase,
        KotlinPoet.lib,
    )
    apiDeps.forEach {
        api(it) {
            excludeSpineBase()
        }
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

    testImplementation(TestLib.lib)
    testImplementation(gradleTestKit())
    testImplementation(ToolBase.pluginTestlib)
}

forceSpineBase()

project.afterEvaluate {
    (tasks.getByName("sourcesJar") as Jar).apply {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

afterEvaluate {
    val kspKotlin by tasks.getting
    val launchSpineCompiler by tasks.getting
    kspKotlin.dependsOn(launchSpineCompiler)
}
