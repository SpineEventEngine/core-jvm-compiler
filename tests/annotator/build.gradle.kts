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

import io.spine.dependency.lib.Grpc
import io.spine.dependency.lib.JavaX

plugins {
    `java-library`
}

spine {
    coreJvm {
        // Turn off validation codegen during the transition to new Compiler API.
        compiler {
            validation.enabled.set(false)
        }

        annotation {
            types {
                internal.set("io.spine.test.annotation.Private")
                experimental.set("io.spine.test.annotation.Attempt")
                beta.set("io.spine.test.annotation.CustomBeta")
                spi.set("io.spine.test.annotation.ServiceProviderInterface")
            }
            internalClassPatterns.addAll(listOf(
                ".*OrBuilder", // Classes ending with `OrBuilder`.
                ".*Proto",     // Classes ending with `Proto`.
                ".*\\.complex\\..*" // Classes which have `.complex.` in their qualified name.
            ))
            internalMethodNames.addAll(listOf(
                "newBuilderForType",
                "parseFrom",
                "parseDelimitedFrom",
                "getSerializedSize",
                "internalGetValueMap"
            ))
        }
    }
}

dependencies {
    val customAnnotations = project(":custom-annotations")
    compileOnlyApi(JavaX.annotations)

    testImplementation(platform(Grpc.bom))
    testImplementation(Grpc.stub)
    testImplementation(Grpc.protobuf)
    testImplementation(customAnnotations)

    spineCompiler(customAnnotations)
}

spine {
    coreJvm {
        grpc {
            enabled.set(true)
        }
    }
}

tasks.findByName("launchTestSpineCompiler")?.apply { this as JavaExec
    debugOptions {
        enabled.set(false) // Set this option to `true` to enable remote debugging.
        port.set(5566)
        server.set(true)
        suspend.set(true)
    }
}
