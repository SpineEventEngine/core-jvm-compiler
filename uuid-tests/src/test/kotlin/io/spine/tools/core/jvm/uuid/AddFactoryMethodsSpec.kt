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

package io.spine.tools.core.jvm.uuid

import io.kotest.matchers.string.shouldContain
import io.spine.tools.core.jvm.GeneratedAnnotation
import io.spine.tools.core.jvm.settings.noParameter
import io.spine.tools.psi.java.method
import java.nio.file.Path
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`AddFactoryMethods` should")
internal class AddFactoryMethodsSpec {

    private val annotationText = GeneratedAnnotation.forPsi().text

    companion object : UuidPluginTestSetup(
        AddFactoryMethods::class.java,
        noParameter
    ) {

        @BeforeAll
        @JvmStatic
        fun setup(@TempDir projectDir: Path) {
            generateCode(projectDir)
        }
    }

    @Test
    fun `add factory method 'generate()'`() {
        generatedCode shouldContain "public static $uuidType generate() {"
        cls.method("generate").text shouldContain annotationText
    }

    @Test
    fun `add factory method 'of()'`() {
        generatedCode shouldContain "public static $uuidType of(String uuid) {"
        cls.method("of").text shouldContain annotationText
    }
}
