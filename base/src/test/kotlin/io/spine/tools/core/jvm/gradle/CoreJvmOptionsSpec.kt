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

package io.spine.tools.core.jvm.gradle

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.spine.annotation.Beta
import io.spine.annotation.Experimental
import io.spine.annotation.Internal
import io.spine.annotation.SPI
import io.spine.tools.compiler.jvm.style.javaCodeStyleDefaults
import io.spine.tools.core.jvm.gradle.given.createCoreJvmOptions
import io.spine.tools.core.jvm.gradle.given.newProject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`CoreJvmOptions` should")
internal class CoreJvmOptionsSpec {

    private lateinit var options: CoreJvmOptions

    @BeforeEach
    fun createOptions() {
        val project = newProject()
        options = project.createCoreJvmOptions()
        options.injectProject(project)
    }

    @Test
    fun `provide the extension name`() {
        CoreJvmOptions.NAME shouldBe "coreJvm"
        CoreJvmOptions.name() shouldBe CoreJvmOptions.NAME
    }

    @Test
    fun `obtain default paths of a project`() {
        val project = newProject()
        CoreJvmOptions.def(project).path() shouldBe project.projectDir.toPath()
    }

    @Test
    fun `use the default Java code style`() {
        options.style.get() shouldBe javaCodeStyleDefaults()
    }

    @Test
    fun `expose compiler settings after project injection`() {
        options.compiler.shouldNotBeNull()
        var configured = false
        options.compiler {
            configured = true
        }
        configured shouldBe true
    }

    @Nested inner class
    `configure annotation settings` {

        @Test
        fun `using standard annotation types by default`() {
            val types = options.annotation.types
            types.experimental.get() shouldBe Experimental::class.java.canonicalName
            types.beta.get() shouldBe Beta::class.java.canonicalName
            types.spi.get() shouldBe SPI::class.java.canonicalName
            types.internal.get() shouldBe Internal::class.java.canonicalName
        }

        @Test
        fun `via the 'annotation' action`() {
            options.annotation {
                it.internalClassPatterns.set(listOf(".*Internal.*"))
            }
            options.annotation.internalClassPatterns.get() shouldBe listOf(".*Internal.*")
        }

        @Test
        fun `via the 'generateAnnotations' action`() {
            options.generateAnnotations {
                it.internalMethodNames.set(listOf("getValue"))
            }
            options.annotation.internalMethodNames.get() shouldBe listOf("getValue")
        }

        @Test
        fun `via the 'types' action`() {
            options.annotation.types {
                it.internal.set("custom.Internal")
            }
            options.annotation.types.internal.get() shouldBe "custom.Internal"
        }
    }
}
