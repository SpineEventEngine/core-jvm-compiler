/*
 * Copyright 2026 CodeMatters, Lda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
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
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME
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

    @Test
    fun `convert itself to Protobuf without resolving the compilation classpath`() {
        settings.toProto()

        val classpath = project.configurations.getByName(COMPILE_CLASSPATH_CONFIGURATION_NAME)
        classpath.state shouldBe Configuration.State.UNRESOLVED
    }

    private companion object {
        const val ACTION = "custom.Action"
        const val FARM_TYPE = "given.base.Farm"
    }
}
