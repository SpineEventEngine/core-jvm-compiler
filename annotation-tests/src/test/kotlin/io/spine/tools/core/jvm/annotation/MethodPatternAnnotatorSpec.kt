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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`MethodPatternAnnotator` should")
internal class MethodPatternAnnotatorSpec {

    companion object : AnnotationPluginTestSetup(
        internalMethodNames = listOf("getValue")
    ) {

        @BeforeAll
        @JvmStatic
        fun setup(@TempDir projectDir: Path) {
            generateCode(projectDir)
        }
    }

    @Test
    fun `annotate methods matching a pattern as 'internal'`() {
        val code = code("PlainOne")
        code.substringBefore("getValue") shouldContain INTERNAL
    }

    @Test
    fun `not annotate methods which do not match`() {
        // The message has the `value` field, so only `getValue` must be annotated,
        // and `getValueBytes` must stay intact.
        val code = code("PlainOne")
        val betweenAccessors = code
            .substringAfter("getValue")
            .substringBefore("getValueBytes")
        betweenAccessors shouldNotContain INTERNAL
    }

    @Test
    fun `not duplicate annotations added by other annotators`() {
        // Accessors of the `value` field of `Carrier` are annotated by `FieldAnnotator`
        // first. The method pattern matches `getValue`, and the annotator must skip it.
        val code = code("Carrier")
        code shouldNotContain Regex("${Regex.escape(INTERNAL)}\\s+${Regex.escape(INTERNAL)}")
    }
}
