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
import io.spine.tools.compiler.Compilation
import io.spine.tools.compiler.ast.event.TypeDiscovered
import io.spine.tools.compiler.protobuf.file
import io.spine.tools.compiler.protobuf.toField
import io.spine.tools.core.jvm.field.given.farmField
import io.spine.tools.validation.event.RequiredFieldDiscovered
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("`RequiredIdReaction` should")
internal class RequiredIdReactionSpec {

    private val reaction = TestReaction()

    @Test
    fun `ignore a field with the explicit 'required' option`() {
        val outcome = reaction.test(farmField("id"), MESSAGE)
        outcome.hasB().shouldBeTrue()
    }

    @Test
    fun `ignore a field of an unsupported type`() {
        val outcome = reaction.test(farmField("rating"), MESSAGE)
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
        val error = assertThrows<Compilation.Error> {
            reaction.test(farmField("empty_id"), MESSAGE)
        }
        error.message.let {
            it shouldContain "empty_id"
            it shouldContain "of type `google.protobuf.Empty`"
            it shouldContain "is assumed to be `(required)` by convention"
            it shouldContain "always equal to the default value"
        }
    }

    @Test
    fun `reject an implicitly required 'repeated' ID field of type 'Empty'`() {
        val error = assertThrows<Compilation.Error> {
            reaction.test(farmField("empty_ids"), MESSAGE)
        }
        error.message.let {
            it shouldContain "empty_ids"
            it shouldContain "of type `repeated google.protobuf.Empty`"
            it shouldContain "is assumed to be `(required)` by convention"
            it shouldContain "always equal to the default value"
        }
    }

    @Test
    fun `reject an implicitly required 'map' ID field with 'Empty' values`() {
        val error = assertThrows<Compilation.Error> {
            reaction.test(farmField("empty_by_name"), MESSAGE)
        }
        error.message.let {
            it shouldContain "empty_by_name"
            it shouldContain "of type `map<string, google.protobuf.Empty>`"
            it shouldContain "is assumed to be `(required)` by convention"
            it shouldContain "always equal to the default value"
        }
    }

    @Test
    fun `not reject a field that does not refer to 'Empty', whatever its type or cardinality`() {
        // None of these refer to `google.protobuf.Empty`, so the field stays implicitly
        // required rather than being rejected:
        //  - `barn`, `barns`, `barns_by_name`: singular, repeated, and mapped messages;
        //  - `tags`, `names_by_id`: repeated and mapped primitives;
        //  - `built`: a non-`Empty` well-known type (`google.protobuf.Timestamp`).
        listOf("barn", "barns", "barns_by_name", "tags", "names_by_id", "built").forEach {
            reaction.test(farmField(it), MESSAGE).hasA().shouldBeTrue()
        }
    }

    private companion object {
        const val MESSAGE = "The ID field must be set."
    }
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
