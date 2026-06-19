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

import com.google.protobuf.Descriptors.FieldDescriptor
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.spine.core.External
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import io.spine.server.tuple.EitherOf2
import io.spine.testing.compiler.assertCompilationError
import io.spine.tools.compiler.ast.event.TypeDiscovered
import io.spine.tools.compiler.protobuf.file
import io.spine.tools.compiler.protobuf.toField
import io.spine.tools.core.jvm.field.given.farmField
import io.spine.tools.validation.event.RequiredFieldDiscovered
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`RequiredIdReaction` should")
internal class RequiredIdReactionSpec {

    private val reaction = TestReaction()

    @Test
    fun `ignore a field with the explicit 'required' option`() {
        val outcome = reaction.test(farmField("id"), MESSAGE)
        outcome.hasB().shouldBeTrue()
    }

    @Test
    fun `ignore an integer ID field, which the '(required)' option does not support`() {
        // `int32` is a valid ID type, so it passes the ID-type check. The `(required)`
        // option does not support numeric types, so the field is left as-is rather
        // than being made implicitly required.
        val outcome = reaction.test(farmField("size"), MESSAGE)
        outcome.hasB().shouldBeTrue()
    }

    @Test
    fun `discover an implicitly required ID field`() {
        val descriptor = farmField("name")
        val outcome = reaction.test(descriptor, MESSAGE)

        outcome.hasA().shouldBeTrue()
        val event = outcome.a
        event.subject shouldBe descriptor.toField()
        event.defaultErrorMessage shouldBe MESSAGE
    }

    @Test
    fun `reject an implicitly required ID field of type 'Empty'`() {
        val (error, output) = assertCompilationError {
            reaction.test(farmField("empty"), MESSAGE)
        }
        error.message.assertErrorContains(
            "empty",
            "of type `google.protobuf.Empty`"
        )
        // The second component carries the captured console output.
        output shouldContain "google.protobuf.Empty"
    }

    @Test
    fun `discover an implicitly required ID field of a singular message type`() {
        // Singular message ID fields are supported and become implicitly required:
        //  - `barn`: a singular message;
        //  - `stall`: a nested message;
        //  - `built`: a non-`Empty` well-known type (`google.protobuf.Timestamp`).
        listOf("barn", "stall", "built").forEach {
            reaction.test(farmField(it), MESSAGE).hasA().shouldBeTrue()
        }
    }

    @Test
    fun `reject a 'repeated' ID field`() {
        // A `repeated` field of any element type is not a supported ID type.
        listOf("tags", "counts", "barns").forEach { name ->
            val (error, _) = assertCompilationError {
                reaction.test(farmField(name), MESSAGE)
            }
            error.message.assertUnsupportedIdType(name)
        }
    }

    @Test
    fun `reject a 'map' ID field`() {
        listOf("barns_by_name", "names_by_id").forEach { name ->
            val (error, _) = assertCompilationError {
                reaction.test(farmField(name), MESSAGE)
            }
            error.message.assertUnsupportedIdType(name)
        }
    }

    @Test
    fun `reject an ID field of an unsupported scalar type`() {
        // `bool`, `bytes`, and `double` are not among the supported ID types.
        listOf("active", "data", "rating").forEach { name ->
            val (error, _) = assertCompilationError {
                reaction.test(farmField(name), MESSAGE)
            }
            error.message.assertUnsupportedIdType(name)
        }
    }

    @Test
    fun `accept an 'enum' ID field`() {
        // An `enum` is a supported ID type, so the ID-type check must pass and the
        // reaction must complete without a compilation error. Whether the field becomes
        // implicitly required depends on `(required)` support for enums.
        val outcome = reaction.test(farmField("color"), MESSAGE)
        (outcome.hasA() || outcome.hasB()).shouldBeTrue()
    }

    private companion object {
        const val MESSAGE = "The ID field must be set."
    }
}

/**
 * Asserts that this nullable String contains the given field name and type description,
 * as well as the standard required field convention messages.
 */
private fun String?.assertErrorContains(fieldName: String, typeDescription: String) {
    this shouldContain fieldName
    this shouldContain typeDescription
    this shouldContain "is assumed to be `(required)` by convention"
    this shouldContain "always equal to the default value"
}

/**
 * Asserts that this nullable String reports an unsupported ID field type
 * for the field with the given name.
 */
private fun String?.assertUnsupportedIdType(fieldName: String) {
    this shouldContain fieldName
    this shouldContain "is not supported"
    this shouldContain "32-bit or 64-bit integer"
}

/**
 * Opens [RequiredIdReaction.withField] for direct calls in tests.
 */
private class TestReaction : RequiredIdReaction() {

    @React
    override fun whenever(
        @External event: TypeDiscovered
    ): EitherOf2<RequiredFieldDiscovered, NoReaction> = ignore()

    fun test(
        field: FieldDescriptor,
        message: String
    ): EitherOf2<RequiredFieldDiscovered, NoReaction> =
        withField(field.toField(), field.file.file(), message)
}
