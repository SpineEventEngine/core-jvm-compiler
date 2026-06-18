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

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.spine.tools.compiler.ast.PrimitiveType
import io.spine.tools.compiler.ast.PrimitiveType.PT_UNKNOWN
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_BOOL
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_BYTES
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_DOUBLE
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_FIXED32
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_FIXED64
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_FLOAT
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_INT32
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_INT64
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_SFIXED32
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_SFIXED64
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_SINT32
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_SINT64
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_STRING
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_UINT32
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_UINT64
import io.spine.tools.compiler.ast.PrimitiveType.UNRECOGNIZED
import io.spine.tools.compiler.ast.TypeName
import io.spine.tools.compiler.ast.fieldType
import io.spine.tools.compiler.ast.mapEntryType
import io.spine.tools.compiler.ast.type
import io.spine.tools.compiler.ast.typeName
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Exhaustively verifies which field types may serve as an ID field.
 *
 * The matrix is driven by [PrimitiveType] itself, so a primitive type added to
 * the Compiler AST in the future is forced into a classification decision.
 *
 * The end-to-end consequences of these classifications — a compilation failure
 * for an unsupported ID field — are covered by `RequiredIdReactionSpec` and the
 * `Unsupported*ErrorSpec` suites.
 */
@DisplayName("`FieldType.isSupportedIdType()` should")
internal class SupportedIdTypeSpec {

    @Test
    fun `classify every declared primitive type`() {
        // If a new `PrimitiveType` is added, this assertion fails until the type
        // is listed in `supported` or `unsupported` below.
        (supported + unsupported) shouldBe allPrimitives
        // No type may be classified as both supported and unsupported.
        (supported intersect unsupported).shouldBeEmpty()
    }

    @Test
    fun `accept 'string' and every 32-bit and 64-bit integer primitive`() {
        supported.forEach { pt ->
            withClue(pt.name) {
                fieldType { primitive = pt }.isSupportedIdType() shouldBe true
            }
        }
    }

    @Test
    fun `reject every other primitive type`() {
        unsupported.forEach { pt ->
            withClue(pt.name) {
                fieldType { primitive = pt }.isSupportedIdType() shouldBe false
            }
        }
    }

    @Test
    fun `accept a singular 'Message' field`() {
        fieldType { message = someTypeName }.isSupportedIdType() shouldBe true
    }

    @Test
    fun `reject an 'enum' field`() {
        fieldType { enumeration = someTypeName }.isSupportedIdType() shouldBe false
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

        /**
         * Primitive types accepted for an ID field: `string` and every 32-bit and
         * 64-bit integer encoding (which map to Java `Integer` and `Long`).
         */
        val supported: Set<PrimitiveType> = setOf(
            TYPE_STRING,
            TYPE_INT32, TYPE_UINT32, TYPE_SINT32, TYPE_FIXED32, TYPE_SFIXED32,
            TYPE_INT64, TYPE_UINT64, TYPE_SINT64, TYPE_FIXED64, TYPE_SFIXED64,
        )

        /** Primitive types that cannot be used for an ID field. */
        val unsupported: Set<PrimitiveType> = setOf(
            PT_UNKNOWN, TYPE_BOOL, TYPE_FLOAT, TYPE_DOUBLE, TYPE_BYTES,
        )

        /** All declared primitive types, excluding the `UNRECOGNIZED` sentinel. */
        val allPrimitives: Set<PrimitiveType> =
            PrimitiveType.entries.toSet() - UNRECOGNIZED

        /** A type name used to synthesize message- and enum-typed fields. */
        val someTypeName: TypeName = typeName {
            packageName = "given.field"
            simpleName = "SomeType"
        }
    }
}
