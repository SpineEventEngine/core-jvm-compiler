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

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_ENUM
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.spine.base.Identifier
import io.spine.tools.compiler.ast.PrimitiveType
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_STRING
import io.spine.tools.compiler.ast.PrimitiveType.UNRECOGNIZED
import io.spine.tools.compiler.ast.TypeName
import io.spine.tools.compiler.ast.fieldType
import io.spine.tools.compiler.ast.mapEntryType
import io.spine.tools.compiler.ast.type
import io.spine.tools.compiler.ast.typeName
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Verifies which field types may serve as an ID field.
 *
 * The supported/unsupported decision is owned by [io.spine.base.Identifier] in the
 * `base-libraries` repository — this suite asserts that the `FieldType.isSupportedIdType()`
 * extension ([isSupportedIdType]) agrees with that single source of truth for every
 * primitive type, and that the structural kinds (`Message`, `enum`, `repeated`, `map`)
 * are dispatched correctly.
 * Iterating over every [PrimitiveType] forces a future primitive into a decision via
 * [Identifier].
 *
 * The end-to-end consequences of these classifications — a compilation failure
 * for an unsupported ID field — are covered by `RequiredIdReactionSpec` and the
 * `Unsupported*ErrorSpec` suites.
 */
@DisplayName("`FieldType.isSupportedIdType()` should")
internal class SupportedIdTypeSpec {

    @Test
    fun `classify every primitive type as 'Identifier' does`() {
        (PrimitiveType.entries - UNRECOGNIZED).forEach { pt ->
            val expected = pt.toProtoType()?.let { Identifier.isSupportedIdType(it) } ?: false
            withClue(pt.name) {
                fieldType { primitive = pt }.isSupportedIdType() shouldBe expected
            }
        }
    }

    @Test
    fun `classify a singular 'Message' field as 'Identifier' does`() {
        fieldType { message = someTypeName }.isSupportedIdType() shouldBe
            Identifier.isSupportedIdType(TYPE_MESSAGE)
    }

    @Test
    fun `classify an 'enum' field as 'Identifier' does`() {
        fieldType { enumeration = someTypeName }.isSupportedIdType() shouldBe
            Identifier.isSupportedIdType(TYPE_ENUM)
    }

    @Test
    fun `reject a 'repeated' field`() {
        fieldType { list = type { primitive = TYPE_STRING } }
            .isSupportedIdType() shouldBe false
    }

    @Test
    fun `reject a 'map' field`() {
        val entry = mapEntryType {
            keyType = TYPE_STRING
            valueType = type { primitive = TYPE_STRING }
        }
        fieldType { map = entry }.isSupportedIdType() shouldBe false
    }

    private companion object {

        /** A type name used to synthesize message- and enum-typed fields. */
        val someTypeName: TypeName = typeName {
            packageName = "given.field"
            simpleName = "SomeType"
        }
    }
}
