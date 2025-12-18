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

package io.spine.tools.core.jvm.entity

import io.spine.annotation.VisibleForTesting
import io.spine.core.External
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import io.spine.server.tuple.EitherOf2
import io.spine.tools.compiler.ast.event.TypeDiscovered
import io.spine.tools.compiler.ast.firstField
import io.spine.tools.compiler.settings.loadSettings
import io.spine.tools.core.jvm.field.RequiredIdReaction
import io.spine.tools.core.jvm.settings.Entities
import io.spine.tools.validation.event.RequiredFieldDiscovered

/**
 * A reaction that marks ID fields in entity state messages as required.
 *
 * The entity state messages are discovered according to settings specified in [Entities].
 * If a message type is annotated with the corresponding [option][Entities.getOptionList],
 * the reaction will mark the first field as required.
 *
 * For such messages, the [TypeDiscovered] event results in [RequiredFieldDiscovered] event
 * which is handled by `ValidationPlugin` of the Validation Compiler.
 */
internal class EntityStateIdReaction : RequiredIdReaction(), EntityPluginComponent {

    private val settings: Entities by lazy {
        loadSettings()
    }

    @React
    @Suppress("ReturnCount") // Prefer sooner exit and precise conditions.
    override fun whenever(
        @External event: TypeDiscovered
    ): EitherOf2<RequiredFieldDiscovered, NoReaction> {
        if (settings.optionList.isEmpty()) {
            return ignore()
        }
        val type = event.type
        if (!type.isEntityState(settings)) {
            return ignore()
        }
        val field = type.firstField
        return withField(field, ID_FIELD_MUST_BE_SET)
    }
}

/**
 * The error message template used when the ID field of an entity state is not set.
 */
@VisibleForTesting
public const val ID_FIELD_MUST_BE_SET: String =
    "The ID field `\${parent.type}.\${field.path}`" +
            " of the type `\${field.type}` must have a non-default value."
