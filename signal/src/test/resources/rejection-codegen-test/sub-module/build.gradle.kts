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

import io.spine.dependency.lib.Protobuf
import io.spine.dependency.local.Base
import io.spine.dependency.local.Validation
import org.gradle.api.tasks.JavaExec

plugins {
    `java-test-fixtures`

    // To allow `modelCompiler` syntax below.
    id("io.spine.core-jvm")
}

// Turn off validation codegen during the transition to new ProtoData API.
modelCompiler {
    java {
        codegen {
            validation.enabled.set(false)
        }
    }
}

dependencies {
    // Add Validation Java Runtime because the generated code reference
    // the `ValidatingBuilder` interface even if validation codegen is turned off.
    implementation(Validation.runtime)

    Protobuf.libs.forEach {
        testFixturesImplementation(it)
    }
    testFixturesImplementation(Base.lib)
    testFixturesImplementation(Validation.runtime)
}

tasks.processResources.get().duplicatesStrategy = DuplicatesStrategy.INCLUDE

tasks.findByName("launchProtoData")?.apply { this as JavaExec
    debugOptions {
        enabled.set(false) // Set this option to `true` to enable remote debugging.
        port.set(5566)
        server.set(true)
        suspend.set(true)
    }
}
