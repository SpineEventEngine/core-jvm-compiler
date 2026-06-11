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

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.spine.tools.core.jvm.field.given.farmDeclaration
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`RepeatedFieldType` should")
internal class RepeatedFieldTypeSpec {

    @Test
    fun `box a primitive component type`() {
        val type = RepeatedFieldType(farmDeclaration("counts"))
        type.name().toString() shouldBe "java.util.List<java.lang.Integer>"
    }

    @Test
    fun `use the declared component type for messages`() {
        val type = RepeatedFieldType(farmDeclaration("barns"))
        type.name().toString() shouldBe
                "java.util.List<io.spine.tools.core.jvm.given.base.Barn>"
        type.toString() shouldBe type.name().toString()
    }

    @Test
    fun `use 'addAll' as the primary setter`() {
        val type = RepeatedFieldType(farmDeclaration("tags"))
        type.primarySetter() shouldBe Accessor.prefix("addAll")
    }

    @Test
    fun `enumerate repeated field accessors`() {
        val type = RepeatedFieldType(farmDeclaration("tags"))
        type.accessors() shouldContainExactlyInAnyOrder setOf(
            Accessor.prefix("get"),
            Accessor.prefixAndPostfix("get", "List"),
            Accessor.prefixAndPostfix("get", "Count"),
            Accessor.prefix("set"),
            Accessor.prefix("add"),
            Accessor.prefix("addAll"),
            Accessor.prefix("clear")
        )
    }
}
