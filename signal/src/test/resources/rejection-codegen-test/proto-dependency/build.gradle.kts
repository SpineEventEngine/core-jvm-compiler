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

import com.google.protobuf.gradle.ProtobufExtension
import io.spine.dependency.lib.Protobuf

// A shared, proto-only module. It only declares Protobuf types and is consumed by `sub-module`
// through the `protobuf()` configuration scope. The `java` and `com.google.protobuf` plugins are
// applied by the root `subprojects` block. Unlike the other modules, it does not apply the
// `io.spine.core-jvm` plugin, so that it stays a plain Protobuf producer and does not export the
// well-known or Spine option types to its consumers. See issue #33.

// Configure the `protoc` executable, which the `io.spine.core-jvm` plugin would otherwise set up.
configure<ProtobufExtension> {
    protoc {
        artifact = Protobuf.compiler
    }
}

dependencies {
    // The Protobuf runtime is needed to compile the Java code generated from this module's
    // own `.proto` file. Spine Base is intentionally not used here (see the note above).
    implementation(Protobuf.javaLib)
}
