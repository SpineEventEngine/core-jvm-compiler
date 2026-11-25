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

package io.spine.tools.core.jvm.entity

import io.spine.core.External
import io.spine.server.event.React
import io.spine.tools.compiler.ast.event.TypeDiscovered
import io.spine.tools.compiler.plugin.Reaction
import io.spine.tools.compiler.settings.loadSettings
import io.spine.tools.core.jvm.entity.event.EntityStateDiscovered
import io.spine.tools.core.jvm.entity.event.entityStateDiscovered
import io.spine.tools.core.jvm.settings.Entities

/**
 * Reacts to the [TypeDiscovered] and emits [EntityStateDiscovered], if the discovered
 * type is an [EntityState][io.spine.base.EntityState].
 *
 * This policy checks if the discovered type has one of the options specified in
 * the [Entities.getOptionList] settings passed to the [EntityPlugin].
 * If so, [EntityStateDiscovered] is emitted. Otherwise, no events are produced.
 *
 * If the query support is generated for entity states, the policy also reports
 * a compilation error for an entity state which breaks the rule on the reserved names.
 *
 * @see ReservedNamesCheck
 * @see Entities
 * @see EntityStateDiscovered
 */
internal class EntityDiscovery : Reaction<TypeDiscovered>(), EntityPluginComponent {

    /**
     * The settings passed by the CoreJvm Compiler to [EntityPlugin].
     */
    private val settings: Entities by lazy {
        loadSettings()
    }

    @React
    override fun whenever(@External event: TypeDiscovered): Iterable<EntityStateDiscovered> {
        val isEntityState = event.type.isEntityState(settings)
        if (isEntityState) {
            checkNames(event)
            return listOf(entityStateDiscovered {
                name = event.type.name
                file = event.file
                type = event.type
            })
        }
        return listOf()
    }

    private fun checkNames(event: TypeDiscovered) {
        if (settings.generateQueries) {
            ReservedNamesCheck(
                state = event.type,
                file = event.file,
                typeSystem = typeSystem
            ).perform()
        }
    }
}
