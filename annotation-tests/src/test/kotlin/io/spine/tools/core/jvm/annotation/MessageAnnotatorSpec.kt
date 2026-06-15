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
import io.kotest.matchers.string.shouldContainOnlyOnce
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Path
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`MessageAnnotator` should")
internal class MessageAnnotatorSpec {

    companion object : AnnotationPluginTestSetup() {

        @BeforeAll
        @JvmStatic
        fun setup(@TempDir projectDir: Path) {
            generateCode(projectDir)
        }
    }

    @Nested inner class
    `annotate top-level message classes` {

        @Test
        fun `with 'Internal' when '(internal_all) = true'`() {
            code("Probe") shouldContain INTERNAL
            code("Sonde") shouldContain INTERNAL
        }

        @Test
        fun `with 'Beta' when '(beta_all) = true'`() {
            code("BetaThing") shouldContain BETA
        }

        @Test
        fun `with 'Experimental' when '(experimental_all) = true'`() {
            code("ExperimentalThing") shouldContain EXPERIMENTAL
        }

        @Test
        fun `with 'SPI' when '(SPI_all) = true'`() {
            code("SpiMessage") shouldContain SPI_ANNOTATION
        }
    }

    @Test
    fun `annotate 'MessageOrBuilder' interfaces`() {
        code("ProbeOrBuilder") shouldContain INTERNAL
        code("BetaThingOrBuilder") shouldContain BETA
    }

    @Test
    fun `annotate a message only once for semantically equal options`() {
        code("DoubleMarked") shouldContainOnlyOnce INTERNAL
    }

    @Test
    fun `not annotate messages without API level options`() {
        code("PlainOne") shouldNotContain INTERNAL
        code("PlainOne") shouldNotContain BETA
    }

    @Test
    fun `not annotate a message reverting the file-wide option`() {
        code("Reverting") shouldNotContain INTERNAL
        code("RevertingOrBuilder") shouldNotContain INTERNAL
    }

    @Test
    fun `not annotate types nested in an outer class annotated via the file-wide option`() {
        val code = code("OuterInternal")
        // The outer class itself carries the annotation...
        code.substringBefore("class OuterInternal") shouldContain INTERNAL
        // ...so the nested types do not repeat it.
        code.substringAfter("class OuterInternal") shouldNotContain INTERNAL
    }
}
