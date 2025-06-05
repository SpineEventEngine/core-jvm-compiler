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

package io.spine.tools.mc.java.entity

import io.spine.protodata.ast.MessageType
import io.spine.protodata.java.render.RenderActions
import io.spine.protodata.java.render.TypeListRenderer
import io.spine.protodata.render.SourceFile
import io.spine.tools.code.Java
import io.spine.tools.mc.java.settings.Entities
import io.spine.tools.psi.java.execute

/**
 * Renders the code for the message types that qualify as [io.spine.base.EntityState].
 *
 * The renderer modifies the code if the [generateQueries][Entities.getGenerateQueries] flag is
 * set to `true` in the code generation settings.
 *
 * The actual code generation is performed by actions [defined][Entities.getActions] in
 * the code generation settings.
 */
public class EntityStateRenderer :
    TypeListRenderer<DiscoveredEntities, Entities>(),
    EntityPluginComponent {

    override fun isEnabled(settings: Entities): Boolean = settings.generateQueries

    override fun doRender(type: MessageType, file: SourceFile<Java>) {
        execute {
            RenderActions(type, file, settings.actions, context).apply()
        }
    }
}
