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

package io.spine.tools.core.jvm.gradle.settings

import com.google.protobuf.StringValue
import com.google.protobuf.stringValue
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.spine.tools.compiler.jvm.render.ImplementInterface
import io.spine.tools.compiler.jvm.render.SuperInterface
import io.spine.tools.core.jvm.gradle.given.newProject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`SettingsWithActions` should")
internal class SettingsWithActionsSpec {

    private lateinit var settings: UuidSettings

    /**
     * The name of the interface action as used by [markAs] methods.
     */
    private val implementInterface = ImplementInterface::class.java.name

    @BeforeEach
    fun createSettings() {
        settings = UuidSettings(newProject())
    }

    private fun action(className: String) =
        settings.actions().actionMap.getValue(className)

    private companion object {
        const val ACTION = "custom.Action"
    }

    @Test
    fun `mark messages with an interface`() {
        settings.markAs("custom.Interface")
        val parameter = action(implementInterface).unpack(SuperInterface::class.java)
        parameter.name shouldBe "custom.Interface"
    }

    @Test
    fun `mark messages with a generic interface`() {
        settings.markAs("custom.Generic", listOf("A", "B"))
        val parameter = action(implementInterface).unpack(SuperInterface::class.java)
        parameter.name shouldBe "custom.Generic"
        parameter.genericArgumentList shouldBe listOf("A", "B")
    }

    @Nested inner class
    `apply custom actions` {

        @Test
        fun `without a parameter`() {
            settings.useAction(ACTION)
            settings.actions().actionMap shouldContainKey ACTION
        }

        @Test
        fun `with a message parameter`() {
            val parameter = stringValue { value = "param" }
            settings.useAction(ACTION, parameter)
            action(ACTION).unpack(StringValue::class.java) shouldBe parameter
        }

        @Test
        fun `with a string parameter`() {
            settings.useAction(ACTION, "param")
            action(ACTION).unpack(StringValue::class.java).value shouldBe "param"
        }

        @Test
        fun `passed as a collection`() {
            settings.useActions(listOf("custom.One", "custom.Two"))
            val map = settings.actions().actionMap
            map shouldContainKey "custom.One"
            map shouldContainKey "custom.Two"
        }

        @Test
        fun `passed as a vararg`() {
            settings.useActions("custom.One", "custom.Two")
            val map = settings.actions().actionMap
            map shouldContainKey "custom.One"
            map shouldContainKey "custom.Two"
        }
    }
}
