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

package io.spine.tools.core.jvm

import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`GeneratedAnnotation` should")
internal class GeneratedAnnotationSpec {

    private val annotationType = "io.spine.annotation.Generated"

    @Test
    fun `create a PSI annotation with a custom value`() {
        val annotation = GeneratedAnnotation.forPsi("custom value")
        annotation.text shouldContain annotationType
        annotation.text shouldContain "\"custom value\""
    }

    @Test
    fun `create a PSI annotation referring to the compiler version by default`() {
        val annotation = GeneratedAnnotation.forPsi()
        annotation.text shouldContain "by Spine CoreJvm Compiler"
    }

    @Test
    fun `create a JavaPoet annotation spec`() {
        val spec = GeneratedAnnotation.forJavaPoet("javapoet value")
        spec.toString() shouldContain annotationType
        spec.toString() shouldContain "\"javapoet value\""
    }

    @Test
    fun `create a KotlinPoet annotation spec`() {
        val spec = GeneratedAnnotation.forKotlinPoet("kotlinpoet value")
        spec.toString() shouldContain "Generated"
        spec.toString() shouldContain "kotlinpoet value"
    }
}
