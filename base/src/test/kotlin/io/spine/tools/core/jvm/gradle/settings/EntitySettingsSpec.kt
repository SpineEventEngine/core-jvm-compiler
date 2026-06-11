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

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.maps.shouldContainKey
import io.spine.tools.core.jvm.gradle.given.newProject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`EntitySettings` should")
internal class EntitySettingsSpec {

    private lateinit var settings: EntitySettings

    @BeforeEach
    fun createSettings() {
        settings = EntitySettings(newProject())
    }

    @Test
    fun `use the 'entity' option by default`() {
        settings.options.get() shouldContain "entity"
    }

    @Test
    fun `generate queries by default`() {
        settings.toProto().generateQueries.shouldBeTrue()
    }

    @Test
    fun `allow skipping query generation`() {
        settings.skipQueries()
        settings.toProto().generateQueries.shouldBeFalse()
    }

    @Test
    fun `allow re-enabling query generation`() {
        settings.skipQueries()
        settings.generateQueries()
        settings.toProto().generateQueries.shouldBeTrue()
    }

    @Test
    fun `convert itself to Protobuf`() {
        val proto = settings.toProto()
        proto.optionList.map { it.name } shouldContain "entity"
        proto.actions.actionMap shouldContainKey
                "io.spine.tools.core.jvm.entity.ImplementEntityState"
    }
}
