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

package io.spine.tools.core.jvm.signal

import io.spine.annotation.VisibleForTesting
import io.spine.core.External
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import io.spine.server.tuple.EitherOf2
import io.spine.tools.compiler.ast.File
import io.spine.tools.compiler.ast.FilePattern
import io.spine.tools.compiler.ast.event.TypeDiscovered
import io.spine.tools.compiler.ast.firstField
import io.spine.tools.compiler.ast.matches
import io.spine.tools.compiler.settings.loadSettings
import io.spine.tools.core.jvm.field.RequiredIdReaction
import io.spine.tools.core.jvm.settings.SignalSettings
import io.spine.tools.validation.event.RequiredFieldDiscovered

/**
 * A reaction that makes the first field in command messages required.
 *
 * By convention in the Spine SDK, the first field of a command messages holds an ID
 * of the target entity. As such, it is required to ensure proper command routing and processing.
 *
 * This reaction takes the list of file patterns for command messages specified
 * in [SignalSettings] to identify a message belonging to a command file.
 * For such messages, the [TypeDiscovered] event results in [RequiredFieldDiscovered] event
 * which is handled by `ValidationPlugin` of the Validation Compiler.
 */
internal class CommandTargetReaction : RequiredIdReaction(), SignalPluginComponent {

    private val settings: SignalSettings by lazy {
        loadSettings()
    }

    private val filePatterns: List<FilePattern> by lazy {
        if (!settingsAvailable()) {
            emptyList()
        } else {
            settings.commands.patternList
        }
    }

    @React
    @Suppress("ReturnCount") // Prefer sooner exit and precise conditions.
    override fun whenever(
        @External event: TypeDiscovered
    ): EitherOf2<RequiredFieldDiscovered, NoReaction> {
        if (filePatterns.isEmpty()) {
            return ignore()
        }
        if (!event.file.matchesPatterns()) {
            return ignore()
        }
        val type = event.type
        val field = type.firstField
        return withField(field, TARGET_ENTITY_ID_MUST_BE_SET)
    }

    private fun File.matchesPatterns(): Boolean =
        filePatterns.any {
            it.matches(this)
        }
}

/**
 * The template for the error message when a field for the target entity ID is not set
 * in a command message.
 */
@VisibleForTesting
public const val TARGET_ENTITY_ID_MUST_BE_SET: String =
    "The ID field of the target entity `\${parent.type}.\${field.path}`" +
            " of the type `\${field.type}` must have a non-default value."
