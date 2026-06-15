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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.spine.tools.core.jvm.field.AddFieldClass
import io.spine.tools.core.jvm.gradle.given.newProject
import io.spine.tools.core.jvm.settings.pattern
import io.spine.tools.core.jvm.settings.typePattern
import io.spine.tools.proto.code.protoTypeName
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`MessageGroupSettings` should")
internal class MessageGroupSettingsSpec {

    private lateinit var settings: MessageGroupSettings

    private val pattern = pattern {
        type = typePattern {
            expectedType = protoTypeName { value = FARM_TYPE }
        }
    }

    @BeforeEach
    fun createSettings() {
        settings = MessageGroupSettings(newProject(), pattern)
    }

    @Test
    fun `require at least one action`() {
        shouldThrow<IllegalStateException> {
            settings.toProto()
        }
    }

    @Test
    fun `mark fields with a custom class`() {
        settings.markFieldsAs("custom.FieldSupertype")
        settings.actions().actionMap shouldContainKey AddFieldClass::class.java.name
    }

    @Test
    fun `convert itself to Protobuf`() {
        settings.useAction(ACTION)
        val proto = settings.toProto()
        proto.pattern shouldBe pattern
        proto.actions.actionMap shouldContainKey ACTION
    }

    @Test
    fun `describe itself with the pattern`() {
        settings.toString() shouldContain FARM_TYPE
    }

    private companion object {
        const val ACTION = "custom.Action"
        const val FARM_TYPE = "given.base.Farm"
    }
}
