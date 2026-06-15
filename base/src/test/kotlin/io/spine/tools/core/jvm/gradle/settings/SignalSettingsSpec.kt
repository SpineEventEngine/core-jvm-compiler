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

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.spine.base.CommandMessage
import io.spine.base.EventMessage
import io.spine.base.MessageFile
import io.spine.base.RejectionMessage
import io.spine.tools.compiler.ast.FilePattern
import io.spine.tools.compiler.ast.FilePatternFactory
import io.spine.tools.compiler.jvm.render.ImplementInterface
import io.spine.tools.compiler.jvm.render.SuperInterface
import io.spine.tools.core.jvm.gradle.given.newProject
import io.spine.tools.java.reference
import org.gradle.api.Project
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`SignalSettings` should")
internal class SignalSettingsSpec {

    private lateinit var project: Project
    private lateinit var settings: CoreJvmCompilerSettings

    @BeforeEach
    fun createSettings() {
        project = newProject()
        settings = CoreJvmCompilerSettings(project)
    }

    private fun interfaceOf(signals: SignalSettings): String {
        val packed = signals.actions().actionMap.getValue(ImplementInterface::class.java.name)
        return packed.unpack(SuperInterface::class.java).name
    }

    @Nested inner class
    `use conventional defaults` {

        @Test
        fun `for commands`() {
            settings.commands.patterns() shouldBe
                    setOf(suffix(MessageFile.COMMANDS))
            interfaceOf(settings.commands) shouldBe CommandMessage::class.java.reference
        }

        @Test
        fun `for events`() {
            settings.events.patterns() shouldBe
                    setOf(suffix(MessageFile.EVENTS))
            interfaceOf(settings.events) shouldBe EventMessage::class.java.reference
        }

        @Test
        fun `for rejections`() {
            settings.rejections.patterns() shouldBe
                    setOf(suffix(MessageFile.REJECTIONS))
            interfaceOf(settings.rejections) shouldBe RejectionMessage::class.java.reference
        }

        private fun suffix(file: MessageFile): FilePattern =
            FilePatternFactory.suffix(file.suffix())
    }

    @Test
    fun `extend the group with more file patterns`() {
        settings.commands.includeFiles(FilePatternFactory.regex(".*my_commands.*"))
        settings.commands.includeFiles(FilePatternFactory.suffix("my_commands.proto"))
        settings.commands.patterns() shouldHaveSize 2
    }

    @Test
    fun `drop the default pattern when the convention is a default instance`() {
        val signals = SignalSettings(
            project = project,
            suffix = "whatever.proto",
            defaultActions = mapOf()
        )
        signals.convention(FilePattern.getDefaultInstance())
        signals.patterns().shouldBeEmpty()
    }

    @Test
    fun `convert itself to Protobuf`() {
        val proto = settings.events.toProto()
        proto.patternList shouldHaveSize 1
        proto.actions.actionMap shouldContainKey ImplementInterface::class.java.name
    }
}
