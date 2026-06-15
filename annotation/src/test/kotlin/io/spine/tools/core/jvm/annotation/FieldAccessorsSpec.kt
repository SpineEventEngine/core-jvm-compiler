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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.spine.tools.compiler.ast.fieldName
import io.spine.tools.compiler.jvm.ClassName
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`FieldAccessors` should")
internal class FieldAccessorsSpec {

    private val className = ClassName("given.test", "Sample")
    private val field = fieldName { value = "my_field" }
    private val insertionPoint = FieldAccessors(className, field)

    private val code = """
        package given.test;

        public class Sample {

            public String getMyField() { return ""; }

            public Sample setMyField(String value) { return this; }

            public int getOther() { return 0; }
        }
        """.trimIndent()

    @Test
    fun `have no label`() {
        insertionPoint.label shouldBe ""
    }

    @Test
    fun `locate all accessor methods of the field`() {
        insertionPoint.locate(code) shouldHaveSize 2
    }

    @Test
    fun `fail when the class is not present in the code`() {
        val otherClass = FieldAccessors(ClassName("given.test", "Missing"), field)
        val exception = shouldThrow<IllegalStateException> {
            otherClass.locate(code)
        }
        exception.message shouldContain "Unable to find the class"
    }

    @Test
    fun `fail when the field has no accessors`() {
        val unknownField = FieldAccessors(className, fieldName { value = "unknown_field" })
        val exception = shouldThrow<IllegalStateException> {
            unknownField.locate(code)
        }
        exception.message shouldContain "Unable to find getter(s)"
    }
}
