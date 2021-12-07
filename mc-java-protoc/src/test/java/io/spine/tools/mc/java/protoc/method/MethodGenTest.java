/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.tools.mc.java.protoc.method;

import io.spine.tools.mc.java.codegen.CodegenOptions;
import io.spine.tools.protoc.plugin.method.EnhancedMessage;
import io.spine.tools.protoc.plugin.method.TestServiceProto;
import io.spine.type.MessageType;
import io.spine.type.ServiceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("`MethodGenerator` should")
final class MethodGenTest {

    @DisplayName("ignore non-`Message` types")
    @Test
    void ignoreNonMessageTypes() {
        var generator = MethodGen.instance(CodegenOptions.getDefaultInstance());
        var service = TestServiceProto.getDescriptor().findServiceByName("MGTService");
        var type = ServiceType.of(service);
        var result = generator.generate(type);
        assertTrue(result.isEmpty());
    }

    @DisplayName("try to generate methods for message types")
    @Test
    void generateMethodsForMessageTypes() {
        var type = new MessageType(EnhancedMessage.getDescriptor());
        var generator = MethodGen.instance(CodegenOptions.getDefaultInstance());
        var result = generator.generate(type);
        assertTrue(result.isEmpty());
    }
}
