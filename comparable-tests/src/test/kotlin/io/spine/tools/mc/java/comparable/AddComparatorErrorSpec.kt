/*
 * Copyright 2025, TeamDev. All rights reserved.
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

package io.spine.tools.mc.java.comparable

import io.kotest.matchers.string.shouldContain
import io.spine.tools.mc.java.comparable.given.BytesProhibited
import io.spine.tools.mc.java.comparable.given.EmptyCompareByOption
import io.spine.tools.mc.java.comparable.given.MapsProhibited
import io.spine.tools.mc.java.comparable.given.NestedBytesProhibited
import io.spine.tools.mc.java.comparable.given.NestedMapsProhibited
import io.spine.tools.mc.java.comparable.given.NestedMessage
import io.spine.tools.mc.java.comparable.given.NestedNonComparableProhibited
import io.spine.tools.mc.java.comparable.given.NestedNonExistingProhibited
import io.spine.tools.mc.java.comparable.given.NestedOneOfProhibited
import io.spine.tools.mc.java.comparable.given.NestedRepeatedProhibited
import io.spine.tools.mc.java.comparable.given.NoCompareByOption
import io.spine.tools.mc.java.comparable.given.NonComparableProhibited
import io.spine.tools.mc.java.comparable.given.NonExistingProhibited
import io.spine.tools.mc.java.comparable.given.OneOfProhibited
import io.spine.tools.mc.java.comparable.given.RepeatedProhibited
import java.nio.file.Path
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`AddComparator` should report an error if '(compare_by)' option refers to")
internal class AddComparatorErrorSpec {

    /**
     * The piece of the error message which lists the supported types.
     */
    private val supportedTypes =
        "Supported field types are: primitives, enums, and comparable messages."

    /**
     * The part of the error message instructing the user to read the docs.
     */
    private val callToAction = "Please see the `(compare_by)` option documentation for details."

    /**
     * The introduction part of the error message telling that maps and repeated fields are
     * not supported for comparison.
     */
    private val repeatedAndMapsProlog = "Repeated fields or maps cannot participate in comparison."

    private lateinit var projectDir: Path

    @BeforeEach
    fun createProjectDir(@TempDir dir: Path) {
        projectDir = dir
    }

    @Test
    fun `empty list of fields`() {
        val error = assertCompilationFails<EmptyCompareByOption>(projectDir)
        error.message.let {
            // The start of the option declaration.
            it shouldContain "/invalid.proto:45:5"
            it shouldContain "The `(compare_by)` option"
            it shouldContain EmptyCompareByOption.getDescriptor().name
            it shouldContain "should have at least one field specified."
        }
    }

    @Test
    fun `a non-comparable message`() {
        val error = assertCompilationFails<NonComparableProhibited>(projectDir)
        error.message.let {
            // The field path.
            it shouldContain "`id`"
            // The type of the message declaring the field.
            it shouldContain NonComparableProhibited.getDescriptor().name
            // The type of the field.
            it shouldContain NoCompareByOption.getDescriptor().name
            // The call to action.
            it shouldContain callToAction
        }
    }

    @Test
    fun `a 'bytes' field`() {
        val error = assertCompilationFails<BytesProhibited>(projectDir)
        error.message.let {
            // The field path.
            it shouldContain "`data`"
            // The declaring message.
            it shouldContain BytesProhibited.getDescriptor().name
            // The type of the field.
            it shouldContain "`bytes`"
        }
    }

    @Test
    fun `a 'repeated' field`() {
        val error = assertCompilationFails<RepeatedProhibited>(projectDir)
        error.message.let {
            val descriptor = RepeatedProhibited.getDescriptor()
            // The prolog of the message.
            it shouldContain repeatedAndMapsProlog
            // The field path.
            it shouldContain "`${descriptor.fullName}.gender`"
            // The declaring message.
            it shouldContain descriptor.name
            // The type of the field.
            it shouldContain "`repeated string`"
            // Reference to the option.
            it shouldContain "`(compare_by)` option"
        }
    }

    @Test
    fun `a 'map' field`() {
        val error = assertCompilationFails<MapsProhibited>(projectDir)
        error.message.let {
            // The prolog of the message.
            it shouldContain repeatedAndMapsProlog
            // The field path.
            it shouldContain "`${MapsProhibited.getDescriptor().fullName}.results`"
            // The type of the field.
            it shouldContain "`map<string, int32>`"
            // Reference to the option.
            it shouldContain "`(compare_by)` option"
        }
    }

    @Test
    fun `an immediate field which does not exist`() {
        val error = assertCompilationFails<NonExistingProhibited>(projectDir)
        error.message.let {
            // Referring to the field belonging directly to the message (not a path).
            it shouldContain "Unable to find a field with the name `non_existing_field`"
            // The declaring type.
            it shouldContain NonExistingProhibited.getDescriptor().fullName
        }
    }

    @Test
    fun `an option under 'oneof' which does not exist`() {
        val error = assertCompilationFails<OneOfProhibited>(projectDir)
        error.message.let {
            // Referring to the field belonging directly to the message (not a path).
            it shouldContain "Unable to find a field with the name `drink`"
            // The declaring type.
            it shouldContain OneOfProhibited.getDescriptor().fullName
        }
    }

    @Nested inner class
    `a nested field which is` {

        @Test
        fun `non-comparable`() {
            val error = assertCompilationFails<NestedNonComparableProhibited>(projectDir)
            error.message.let {
                // Referring to a field path.
                it shouldContain "The field `nested.id`"
                // The declaring message.
                it shouldContain NestedNonComparableProhibited.getDescriptor().fullName
                // The type of the field.
                it shouldContain NoCompareByOption.getDescriptor().fullName

                it shouldContain supportedTypes
                it shouldContain callToAction
            }
        }

        @Test
        fun `'bytes' field`() {
            val error = assertCompilationFails<NestedBytesProhibited>(projectDir)
            error.message.let {
                // Referring to a field path.
                it shouldContain "The field `nested.data`"
                // The declaring message.
                it shouldContain NestedBytesProhibited.getDescriptor().fullName
                // The type of the field.
                it shouldContain "bytes"
            }
        }

        @Test
        fun `'repeated' field`() {
            val error = assertCompilationFails<NestedRepeatedProhibited>(projectDir)
            error.message.let {
                val descriptor = NestedMessage.getDescriptor()
                // The prolog of the message.
                it shouldContain repeatedAndMapsProlog
                // The field qualified name.
                it shouldContain "`${descriptor.fullName}.gender`"
                // The declaring message.
                it shouldContain descriptor.name
                // The type of the field.
                it shouldContain "`repeated string`"
                // Reference to the option.
                it shouldContain "`(compare_by)` option"
            }
        }

        @Test
        fun `'map' field`() {
            val error = assertCompilationFails<NestedMapsProhibited>(projectDir)
            error.message.let {
                val descriptor = NestedMessage.getDescriptor()
                // The prolog of the message.
                it shouldContain repeatedAndMapsProlog
                // The field qualified name.
                it shouldContain "`${descriptor.fullName}.results`"
                // The declaring message.
                it shouldContain descriptor.name
                // The type of the field.
                it shouldContain "`map<string, int32>`"
                // Reference to the option.
                it shouldContain "`(compare_by)` option"
            }
        }

        @Test
        fun `non-existing field`() {
            val error = assertCompilationFails<NestedNonExistingProhibited>(projectDir)
            error.message.let {
                // Referring to the field belonging to a nested message.
                it shouldContain "Unable to find a field with the path `nested.non_existing_field`"
                // The declaring type.
                it shouldContain NestedNonExistingProhibited.getDescriptor().fullName
            }
        }

        @Test
        fun `'oneof' field`() {
            val error = assertCompilationFails<NestedOneOfProhibited>(projectDir)
            error.message.let {
                // Referring to the field belonging to the nested message.
                it shouldContain "Unable to find a field with the path `nested.drink`"
                // The declaring type.
                it shouldContain NestedOneOfProhibited.getDescriptor().fullName
            }
        }
    }
}
