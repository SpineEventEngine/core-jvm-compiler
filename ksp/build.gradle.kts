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

import io.spine.dependency.build.Ksp
import io.spine.dependency.lib.AutoService
import io.spine.dependency.lib.Kotlin
import io.spine.dependency.lib.Protobuf
import io.spine.dependency.local.ProtoData
import io.spine.dependency.local.TestLib
import io.spine.dependency.local.ToolBase
import io.spine.dependency.test.Kotest

@Suppress("unused")
val compileClasspath by configurations.getting {
    resolutionStrategy.force(
        Kotlin.Compiler.embeddable,
        Kotlin.scriptRuntime
    )
}

dependencies {
    // The dependencies of the processor part.
    compileOnlyApi(Kotlin.Compiler.embeddable)
    api(Ksp.artifact(Ksp.symbolProcessingApi))
    api(AutoService.annotations)

    implementation(Ksp.artifact(Ksp.symbolProcessingAaEmb))?.because(
        "It was not resolved automatically by KSP Gradle Plugin in integration tests." +
                " We need go re-visit this in future versions of KSP Gradle Plugin."
    )

    // The dependencies for the Gradle plugin part.
    compileOnlyApi(gradleApi())
    compileOnlyApi(gradleKotlinDsl())
    compileOnlyApi(Kotlin.GradlePlugin.lib)

    api(ToolBase.pluginBase)
    api(ProtoData.gradleApi)?.because(
        "We want KSP-based plugins use this API directly."
    )
    api(Ksp.artifact(Ksp.gradlePlugin))?.because(
        "This is `api` dependency because we add this plugin from our code" +
                " and want its API being visible to users."
    )
    implementation(Protobuf.GradlePlugin.lib)?.because(
        "We need `ProtobufExtension` for ignoring `generated/sources/proto/` directory."
    )

    // Test dependencies.
    arrayOf(
        gradleTestKit(),
        gradleKotlinDsl(),
        Kotlin.GradlePlugin.lib,
        TestLib.lib,
        Kotest.assertions
    ).forEach {
        testImplementation(it)
    }
}
