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
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.spine.tools.compiler.ast.FilePatternFactory
import io.spine.tools.core.jvm.gradle.given.newProject
import org.gradle.api.Project
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`CoreJvmCompilerSettings` should")
internal class CoreJvmCompilerSettingsSpec {

    private lateinit var project: Project
    private lateinit var settings: CoreJvmCompilerSettings

    @BeforeEach
    fun createSettings() {
        project = newProject()
        project.pluginManager.apply("java")
        settings = CoreJvmCompilerSettings(project)
    }

    @Test
    fun `be enabled by default`() {
        settings.enabled.get().shouldBeTrue()
    }

    @Test
    fun `expose a file pattern factory`() {
        settings.by() shouldBe FilePatternFactory
    }

    @Nested inner class
    `configure code generation` {

        @Test
        fun `for commands`() {
            settings.forCommands {
                it.includeFiles(settings.by().suffix("custom_commands.proto"))
                it.includeFiles(settings.by().regex(".*cmd.*"))
            }
            settings.commands.patterns() shouldHaveSize 2
        }

        @Test
        fun `for events`() {
            var configured = false
            settings.forEvents { configured = true }
            configured.shouldBeTrue()
        }

        @Test
        fun `for rejections`() {
            var configured = false
            settings.forRejections { configured = true }
            configured.shouldBeTrue()
        }

        @Test
        fun `for entities`() {
            var configured = false
            settings.forEntities { configured = true }
            configured.shouldBeTrue()
        }

        @Test
        fun `for UUID values`() {
            var configured = false
            settings.forUuids { configured = true }
            configured.shouldBeTrue()
        }

        @Test
        fun `for comparable messages`() {
            var configured = false
            settings.forComparables { configured = true }
            configured.shouldBeTrue()
        }

        @Test
        fun `for messages selected by a file pattern`() {
            settings.forMessages(settings.by().suffix("ids.proto")) {
                it.useAction(ACTION)
            }
            settings.messageGroups shouldHaveSize 1
        }

        @Test
        fun `for a message selected by its type name`() {
            settings.forMessage(FARM_TYPE) {
                it.useAction(ACTION)
            }
            settings.messageGroups shouldHaveSize 1
        }

        @Test
        fun `rejecting a message group without actions`() {
            shouldThrow<IllegalStateException> {
                settings.forMessages(settings.by().suffix("ids.proto")) {
                    // No actions are configured.
                }
            }
        }
    }

    @Test
    fun `convert itself to Protobuf`() {
        settings.forMessage(FARM_TYPE) {
            it.useAction(ACTION)
        }
        val proto = settings.toProto()

        proto.hasSignalSettings().shouldBeTrue()
        proto.signalSettings.hasCommands().shouldBeTrue()
        proto.signalSettings.hasEvents().shouldBeTrue()
        proto.signalSettings.hasRejections().shouldBeTrue()
        proto.hasEntities().shouldBeTrue()
        proto.hasUuids().shouldBeTrue()
        proto.hasComparables().shouldBeTrue()
        proto.groupSettings.groupList shouldHaveSize 1
        proto.entities.actions.actionMap shouldContainKey
                "io.spine.tools.core.jvm.entity.ImplementEntityState"
    }

    private companion object {
        const val ACTION = "custom.Action"
        const val FARM_TYPE = "given.base.Farm"
    }
}
