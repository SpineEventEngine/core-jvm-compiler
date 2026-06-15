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

package io.spine.tools.core.jvm.base

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.spine.base.fieldPath
import io.spine.tools.compiler.protobuf.toMessageType
import io.spine.tools.core.jvm.field.given.farmTypeSystem
import io.spine.tools.core.jvm.given.base.Farm
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Extensions for `FieldPath` should")
internal class FieldPathExtsSpec {

    private val typeSystem = farmTypeSystem()
    private val farm = Farm.getDescriptor().toMessageType()

    private fun path(vararg names: String) = fieldPath {
        fieldName.addAll(names.toList())
    }

    @Nested inner class
    `tell if a path is not nested` {

        @Test
        fun `for a single-element path`() {
            path("name").isNotNested.shouldBeTrue()
        }

        @Test
        fun `for a multi-element path`() {
            path("barn", "title").isNotNested.shouldBeFalse()
        }
    }

    @Test
    fun `join the path with dots`() {
        path("barn", "stall", "id").joined shouldBe "barn.stall.id"
    }

    @Nested inner class
    `obtain the root field name` {

        @Test
        fun `of a non-empty path`() {
            path("barn", "title").root shouldBe "barn"
        }

        @Test
        fun `rejecting an empty path`() {
            shouldThrow<NoSuchElementException> {
                path().root
            }
        }
    }

    @Nested inner class
    `resolve a field path` {

        @Test
        fun `pointing to a direct field`() {
            val field = typeSystem.resolve(path("name"), farm)
            field.name.value shouldBe "name"
        }

        @Test
        fun `pointing to a nested field`() {
            val field = typeSystem.resolve(path("barn", "stall", "id"), farm)
            field.name.value shouldBe "id"
        }

        @Test
        fun `rejecting a path through a non-message field`() {
            val exception = shouldThrow<IllegalStateException> {
                typeSystem.resolve(path("name", "whatever"), farm)
            }
            exception.message shouldContain "doesn't denote a message"
        }

        @Test
        fun `rejecting a path through an unknown message type`() {
            val exception = shouldThrow<IllegalStateException> {
                typeSystem.resolve(path("built", "seconds"), farm)
            }
            exception.message shouldContain "was not found"
        }
    }
}
