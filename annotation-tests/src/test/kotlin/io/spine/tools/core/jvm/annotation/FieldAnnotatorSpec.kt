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

package io.spine.tools.core.jvm.annotation

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Path
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`FieldAnnotator` should")
internal class FieldAnnotatorSpec {

    companion object : AnnotationPluginTestSetup() {

        @BeforeAll
        @JvmStatic
        fun setup(@TempDir projectDir: Path) {
            generateCode(projectDir)
        }
    }

    @Nested inner class
    `annotate field accessors` {

        @Test
        fun `in a message class and its builder`() {
            val code = code("Carrier")
            code shouldContain INTERNAL
            // The annotation goes right above the accessors of the `value` field.
            code.substringBefore("getValue") shouldContain INTERNAL
        }

        @Test
        fun `in the 'MessageOrBuilder' interface`() {
            code("CarrierOrBuilder") shouldContain INTERNAL
        }

        @Test
        fun `of repeated fields`() {
            val code = code("Carrier")
            code.substringBefore("getTagsList") shouldContain BETA
        }

        @Test
        fun `of map fields`() {
            val code = code("Carrier")
            code.substringBefore("getAttrsMap") shouldContain EXPERIMENTAL
        }

        @Test
        fun `with several annotations for a field with several options`() {
            val code = code("Carrier")
            val beforeBoth = code.substringBefore("getBothBytes")
            beforeBoth shouldContain INTERNAL
            beforeBoth shouldContain BETA
        }

        @Test
        fun `of fields enclosed in an outer class`() {
            code("InternalFieldSingle") shouldContain INTERNAL
        }
    }

    @Test
    fun `not annotate accessors of fields without API level options`() {
        // The `PlainOne` message has the `value` field with no options.
        code("PlainOne") shouldNotContain INTERNAL
    }

    @Test
    fun `not annotate accessors of a field with the option set to 'false'`() {
        // The `off` field of the `Reverting` message has `(internal) = false`.
        code("Reverting") shouldNotContain INTERNAL
    }
}
