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

package io.spine.tools.core.jvm.settings

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.spine.tools.compiler.ast.FilePatternFactory
import io.spine.tools.compiler.protobuf.toMessageType
import io.spine.tools.core.jvm.given.base.Farm
import io.spine.tools.proto.code.protoTypeName
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`Patterns` should")
internal class PatternsSpec {

    private val farm = Farm.getDescriptor().toMessageType()
    private val farmName = "given.base.Farm"

    @Nested inner class
    `match a pattern by` {

        @Test
        fun `the file of the type`() {
            val matching = pattern {
                file = FilePatternFactory.suffix("farm.proto")
            }
            val nonMatching = pattern {
                file = FilePatternFactory.suffix("barn.proto")
            }
            matching.matches(farm).shouldBeTrue()
            nonMatching.matches(farm).shouldBeFalse()
        }

        @Test
        fun `the type pattern`() {
            val matching = pattern {
                type = typePattern {
                    expectedType = protoTypeName { value = farmName }
                }
            }
            matching.matches(farm).shouldBeTrue()
        }

        @Test
        fun `nothing when the kind is not set`() {
            Pattern.getDefaultInstance().matches(farm).shouldBeFalse()
        }
    }

    @Nested inner class
    `match a type pattern by` {

        @Test
        fun `the expected type name`() {
            val matching = typePattern {
                expectedType = protoTypeName { value = farmName }
            }
            val nonMatching = typePattern {
                expectedType = protoTypeName { value = "given.base.Unknown" }
            }
            matching.matches(farm).shouldBeTrue()
            nonMatching.matches(farm).shouldBeFalse()
        }

        @Test
        fun `a regular expression`() {
            val matching = typePattern { regex = ".*base\\.F.*" }
            val nonMatching = typePattern { regex = ".*base\\.B.*" }
            matching.matches(farm).shouldBeTrue()
            nonMatching.matches(farm).shouldBeFalse()
        }

        @Test
        fun `nothing when the value is not set`() {
            TypePattern.getDefaultInstance().matches(farm).shouldBeFalse()
        }
    }
}
