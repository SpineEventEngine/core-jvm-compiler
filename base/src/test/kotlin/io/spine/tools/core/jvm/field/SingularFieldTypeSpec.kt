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

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.spine.tools.core.jvm.field.given.farmDeclaration
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`SingularFieldType` should")
internal class SingularFieldTypeSpec {

    @Test
    fun `unbox a primitive type name`() {
        SingularFieldType(farmDeclaration("size")).name().toString() shouldBe "int"
    }

    @Test
    fun `convert a nested binary class name to the dot notation`() {
        val type = SingularFieldType(farmDeclaration("stall"))
        type.name().toString() shouldBe "io.spine.tools.core.jvm.given.base.Barn.Stall"
    }

    @Test
    fun `expose extra accessors for string fields`() {
        val accessors = SingularFieldType(farmDeclaration("name")).accessors()
        accessors shouldContain Accessor.prefixAndPostfix("get", "Bytes")
        accessors shouldContain Accessor.prefixAndPostfix("set", "Bytes")
    }

    @Test
    fun `not expose string accessors for other types`() {
        val accessors = SingularFieldType(farmDeclaration("size")).accessors()
        accessors shouldNotContain Accessor.prefixAndPostfix("get", "Bytes")
        accessors shouldContain Accessor.prefix("has")
        accessors shouldContain Accessor.prefix("get")
        accessors shouldContain Accessor.prefix("set")
        accessors shouldContain Accessor.prefix("clear")
    }

    @Test
    fun `use 'set' as the primary setter`() {
        SingularFieldType(farmDeclaration("name")).primarySetter() shouldBe
                Accessor.prefix("set")
    }

    @Test
    fun `support enum fields`() {
        val type = SingularFieldType(farmDeclaration("color"))
        type.name().toString() shouldBe "io.spine.tools.core.jvm.given.base.Color"
    }
}
