/*
 * Copyright 2024, TeamDev. All rights reserved.
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

import io.spine.core.External
import io.spine.protodata.ast.event.TypeDiscovered
import io.spine.protodata.plugin.Policy
import io.spine.protodata.settings.loadSettings
import io.spine.server.event.React
import io.spine.tools.mc.java.entity.event.EntityStateDiscovered
import io.spine.tools.mc.java.entity.event.entityStateDiscovered
import io.spine.tools.mc.java.settings.Entities

/**
 * Reacts to the [TypeDiscovered] and emits [EntityStateDiscovered], if the discovered
 * type is an [EntityState][io.spine.base.EntityState].
 *
 * This policy checks if the discovered type has one of the options specified in
 * the [Entities.getOptionList] settings passed to the [EntityPlugin].
 * If so, [EntityStateDiscovered] is emitted. Otherwise, no events are produced.
 *
 * @see Entities
 * @see EntityStateDiscovered
 */
internal class EntityDiscovery : Policy<TypeDiscovered>(), EntityPluginComponent {

    /**
     * The settings passed by McJava to [EntityPlugin].
     */
    private val settings: Entities by lazy {
        loadSettings()
    }

    @React
    override fun whenever(@External event: TypeDiscovered): Iterable<EntityStateDiscovered> {
        val isEntityState = event.type.isEntityState(settings)
        if (isEntityState) {
            return listOf(entityStateDiscovered {
                name = event.type.name
                file = event.file
                type = event.type
            })
        }
        return listOf()
    }
}
