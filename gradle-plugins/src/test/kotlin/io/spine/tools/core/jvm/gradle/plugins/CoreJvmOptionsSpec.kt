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

package io.spine.tools.core.jvm.gradle.plugins

import com.google.common.truth.Truth
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.spine.base.MessageFile
import io.spine.option.OptionsProto
import io.spine.testing.SlowTest
import io.spine.tools.compiler.jvm.render.ImplementInterface
import io.spine.tools.compiler.render.actions
import io.spine.tools.compiler.render.add
import io.spine.tools.core.jvm.NoOpMessageAction
import io.spine.tools.core.jvm.applyStandard
import io.spine.tools.core.jvm.field.AddFieldClass
import io.spine.tools.core.jvm.gradle.CoreJvmOptions
import io.spine.tools.core.jvm.gradle.coreJvmOptions
import io.spine.tools.core.jvm.gradle.settings.EntitySettings
import io.spine.tools.core.jvm.gradle.settings.SignalSettings.Companion.DEFAULT_COMMAND_ACTIONS
import io.spine.tools.core.jvm.gradle.settings.SignalSettings.Companion.DEFAULT_EVENT_ACTIONS
import io.spine.tools.core.jvm.gradle.settings.SignalSettings.Companion.DEFAULT_REJECTION_ACTIONS
import io.spine.tools.core.jvm.gradle.settings.UuidSettings
import io.spine.tools.core.jvm.settings.MessageGroup
import io.spine.tools.core.jvm.settings.Pattern
import io.spine.tools.core.jvm.settings.SignalSettings
import io.spine.tools.core.jvm.settings.TypePattern
import io.spine.tools.kotlin.reference
import io.spine.tools.proto.code.ProtoTypeName
import java.io.File
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.CleanupMode
import org.junit.jupiter.api.io.TempDir

@SlowTest
@DisplayName("`CoreJvmOptions` should`")
class CoreJvmOptionsSpec {

    private lateinit var options: CoreJvmOptions
    private lateinit var projectDir: File

    /**
     * Calculates the [io.spine.tools.core.jvm.settings.SignalSettings] after
     * [options] are modified by a test body.
     */
    private val signalSettings: SignalSettings
        get() = options.compiler!!.toProto().signalSettings

    /**
     * Creates the project in the given directory.
     *
     * The directory is set not to be cleaned up by JUnit because cleanup sometimes
     * fails under Windows.
     * See [this comment](https://github.com/gradle/gradle/issues/12535#issuecomment-1064926489)
     * on the corresponding issue for details:
     *
     * The [projectDir] is set to be removed in the [removeTempDir] method.
     *
     * @see removeTempDir
     */
    @BeforeEach
    fun prepareExtension(
        @TempDir(cleanup = CleanupMode.NEVER) projectDir: File
    ) {
        this.projectDir = projectDir
        val project = ProjectBuilder.builder()
            .withProjectDir(projectDir)
            .build()
        // Add repositories for resolving locally built artifacts (via `mavenLocal()`)
        // and their dependencies via `mavenCentral()`.
        project.repositories.applyStandard()
        project.apply {
            it.plugin("java")
            it.plugin("com.google.protobuf")
            it.plugin(CoreJvmPlugin::class.java)
        }
        options = project.coreJvmOptions
    }

    @AfterEach
    fun removeTempDir() {
        projectDir.deleteOnExit()
    }

    @Test
    fun `apply changes immediately`() {
        val actionName = "fake.Action"
        options.compiler { settings ->
            settings.forUuids {
                it.useAction(actionName)
            }
        }
        val settings = options.compiler!!.toProto()

        settings.uuids.actions.actionMap.keys shouldBe
                UuidSettings.DEFAULT_ACTIONS.keys + actionName
    }

    @Nested
    @DisplayName("configure generation of")
    inner class ConfigureGeneration {

        @Test
        fun commands() {
            val action1 = "org.example.command.codegen.Action1"
            val action2 = "org.example.command.codegen.Action2"
            val suffix = "_my_commands.proto"
            options.compiler { settings ->
                settings.forCommands { commands ->
                    with(commands) {
                        includeFiles(by().suffix(suffix))
                        useActions(action1)
                        useActions(action2)
                    }
                }
            }

            signalSettings.commands.run {
                patternList shouldHaveSize 1
                patternList[0].suffix shouldBe suffix
                actions.actionMap.keys shouldBe
                        DEFAULT_COMMAND_ACTIONS.keys + setOf(action1, action2)
            }
        }

        @Test
        fun events() {
            val action1 = "org.example.event.codegen.Action1"
            val action2 = "org.example.event.codegen.Action2"
            val infix = "my_"
            options.compiler { settings ->
                settings.forEvents { events ->
                    with(events) {
                        includeFiles(by().infix(infix))
                        useActions(action1, action2)
                    }
                }
            }

            signalSettings.events.run {
                patternList shouldHaveSize 1
                patternList[0].infix shouldBe infix
                actions.actionMap.keys shouldBe
                        DEFAULT_EVENT_ACTIONS.keys + setOf(action1, action2)
            }
        }

        @Test
        fun rejections() {
            val action1 = "org.example.rejection.codegen.Action1"
            val action2 = "org.example.rejection.codegen.Action2"
            val regex = ".*rejection.*"
            options.compiler { settings ->
                settings.forRejections { rejections ->
                    rejections.includeFiles(rejections.by().regex(regex))
                    rejections.useActions(listOf(action1, action2))
                }
            }

            signalSettings.rejections.run {
                patternList shouldHaveSize 1
                patternList[0].regex shouldBe regex
                actions.actionMap.keys shouldBe
                        DEFAULT_REJECTION_ACTIONS.keys + setOf(action1, action2)
            }
        }

        @Test
        fun `rejections separately from events`() {
            val eventAction = "org.example.event.Action"
            val rejectionAction = "org.example.rejection.Action"
            options.compiler { settings ->
                settings.forEvents {
                    it.useAction(eventAction)
                }
                settings.forRejections {
                    it.useActions(rejectionAction)
                }
            }

            signalSettings.events.actions.actionMap.keys shouldBe
                    DEFAULT_EVENT_ACTIONS.keys + eventAction
            signalSettings.rejections.actions.actionMap.keys shouldBe
                    DEFAULT_REJECTION_ACTIONS.keys + rejectionAction
        }

        @Test
        fun entities() {
            val action = "custom.Action"
            val option = "view"
            options.compiler { settings ->
                settings.forEntities {
                    it.options.add(option)
                    it.skipQueries()
                    it.useAction(action)
                }
            }
            val entities = options.compiler!!.toProto().entities

            entities.run {
                actions.actionMap.keys shouldContainExactly
                        EntitySettings.DEFAULT_ACTIONS.keys + action

                optionList shouldHaveSize 1
                optionList.first().name shouldBe option
            }
        }

        @Test
        fun `UUID messages`() {
            val customAction = "custom.UuidCodegenAction"
            options.compiler { settings ->
                settings.forUuids {
                    it.useAction(customAction)
                }
            }
            val uuids = options.compiler!!.toProto().uuids
            uuids.run {
                actions.actionMap.keys shouldBe UuidSettings.DEFAULT_ACTIONS.keys + customAction
            }
        }

        @Test
        fun `arbitrary message groups`() {
            val firstInterface = "com.acme.Foo"
            val secondInterface = "com.acme.Bar"
            val nestedClassAction = "custom.NestedClassAction"
            val anotherNestedClassAction = "custom.AnotherNestedClassAction"
            val fieldSuperclass = "acme.Searchable"
            val firstMessageType = "acme.small.yellow.Bird"
            options.compiler { settings ->
                settings.forMessage(firstMessageType) {
                    it.markAs(firstInterface)
                    it.markFieldsAs(fieldSuperclass)
                    it.useAction(nestedClassAction)
                }
                settings.forMessages(settings.by().regex(".+_.+")) {
                    it.markAs(secondInterface)
                    it.useAction(anotherNestedClassAction)
                }
            }
            val groups = options.compiler!!.toProto().groupSettings.groupList

            groups shouldHaveSize 2

            var (first, second) = groups

            // Restore ordering. When generating code, it does not matter which group goes
            // after which.
            if (second.pattern.hasType()) {
                val t = second
                second = first
                first = t
            }

            first.run {
                pattern.type.expectedType.value shouldBe firstMessageType
                actions.actionMap.keys shouldBe setOf(
                    ImplementInterface::class.java.name,
                    AddFieldClass::class.java.name,
                    nestedClassAction
                )
            }

            second.run {
                pattern.file.hasRegex() shouldBe true
                actions.actionMap.keys shouldBe setOf(
                    ImplementInterface::class.java.name,
                    anotherNestedClassAction
                )
            }
        }
    }

    @Nested
    @DisplayName("provide reasonable defaults for")
    inner class ProvideDefaults {

        @Test
        fun commands() {
            signalSettings.commands.run {
                patternList shouldHaveSize 1
                patternList[0].suffix shouldBe MessageFile.COMMANDS.suffix()
            }
        }

        @Test
        fun events() {
            signalSettings.events.run {
                patternList shouldHaveSize 1
                patternList[0].suffix shouldBe MessageFile.EVENTS.suffix()
            }
        }

        @Test
        fun rejections() {
            signalSettings.rejections.run {
                patternList shouldHaveSize 1
                patternList[0].suffix shouldBe MessageFile.REJECTIONS.suffix()
            }
        }

        @Test
        fun entities() {
            val entities = options.compiler!!.toProto().entities

            entities.run {
                optionList shouldHaveSize 1
                optionList.first().name shouldBe OptionsProto.entity.descriptor.name
                actions.actionMap.keys shouldContainExactlyInAnyOrder
                        EntitySettings.DEFAULT_ACTIONS.keys
            }
        }

        @Test
        fun `arbitrary message groups`() {
            val settings = options.compiler!!.toProto()

            settings.groupSettings.groupList shouldBe emptyList()

            val stubActionClass = NoOpMessageAction::class
            val type = "test.Message"
            options.compiler {
                it.forMessage(type) { group ->
                    group.useAction(stubActionClass.reference)
                }
            }
            val updated = options.compiler!!.toProto()

            updated.groupSettings.groupList shouldHaveSize 1
            val typeName = ProtoTypeName.newBuilder().setValue(type)
            val typePattern = TypePattern.newBuilder()
                .setExpectedType(typeName)
            val pattern = Pattern.newBuilder()
                .setType(typePattern)

            updated.groupSettings.groupList.first() shouldBe
                    MessageGroup.newBuilder()
                        .setPattern(pattern)
                        .setActions(actions {
                            add(stubActionClass)
                        })
                        .buildPartial()
        }

        @Test
        fun validation() {
            val validation = options.compiler!!.toProto().validation
            validation.run {
                version.shouldBeEmpty()
            }
        }
    }

    @Nested
    @DisplayName("allow configuring generation of queries")
    inner class AllowConfiguring {

        @Test
        fun `having queries turned by default`() {
            assertFlag().isTrue()
        }

        @Test
        fun `turning generation of queries off`() {
            options.compiler!!.forEntities {
                it.skipQueries()
            }
            assertFlag().isFalse()
        }

        @Test
        fun `turning generation of queries on`() {
            // Turn `off`, assuming that the default is `on`.
            options.compiler!!.forEntities {
                it.skipQueries()
            }

            // Turn `on`.
            options.compiler!!.forEntities {
                it.generateQueries()
            }

            assertFlag().isTrue()
        }

        private fun assertFlag() =
            Truth.assertThat(options.compiler!!.toProto().entities.generateQueries)
    }
}
