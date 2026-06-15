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

package io.spine.tools.core.jvm.field

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.spine.tools.core.jvm.field.given.farmDeclaration
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`MapFieldType` should")
internal class MapFieldTypeSpec {

    @Test
    fun `box a primitive key type`() {
        val type = MapFieldType(farmDeclaration("names_by_id"))
        type.name().toString() shouldBe
                "java.util.Map<java.lang.Integer, java.lang.String>"
    }

    @Test
    fun `use declared types for non-primitive entries`() {
        val type = MapFieldType(farmDeclaration("barns_by_name"))
        type.name().toString() shouldBe
                "java.util.Map<java.lang.String, io.spine.tools.core.jvm.given.base.Barn>"
        type.toString() shouldBe type.name().toString()
    }

    @Test
    fun `use 'putAll' as the primary setter`() {
        val type = MapFieldType(farmDeclaration("names_by_id"))
        type.primarySetter() shouldBe Accessor.prefix("putAll")
    }

    @Test
    fun `enumerate map accessors`() {
        val type = MapFieldType(farmDeclaration("names_by_id"))
        type.accessors() shouldContainAll setOf(
            Accessor.prefix("get"),
            Accessor.prefixAndPostfix("get", "Map"),
            Accessor.prefixAndPostfix("get", "OrDefault"),
            Accessor.prefixAndPostfix("get", "OrThrow"),
            Accessor.prefix("contains"),
            Accessor.prefix("clear"),
            Accessor.prefix("put"),
            Accessor.prefix("remove"),
            Accessor.prefix("putAll")
        )
    }

    @Test
    fun `reject a non-map field`() {
        shouldThrow<IllegalArgumentException> {
            MapFieldType(farmDeclaration("name"))
        }
    }
}
