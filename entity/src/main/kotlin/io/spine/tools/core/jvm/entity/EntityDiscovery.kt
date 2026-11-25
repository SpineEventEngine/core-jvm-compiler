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
import io.spine.tools.compiler.Compilation
import io.spine.tools.compiler.ast.File
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.compiler.ast.Option
import io.spine.tools.compiler.ast.event.TypeDiscovered
import io.spine.tools.compiler.ast.isTopLevel
import io.spine.tools.compiler.ast.qualifiedName
import io.spine.tools.compiler.check
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
 * An entity state must be a top-level message. If it is nested in another message,
 * the reaction reports a compilation error.
 *
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
        val option = event.type.findEntityOption(settings)
            ?: return listOf()
        checkTopLevel(event.type, option, event.file)
        return listOf(entityStateDiscovered {
            name = event.type.name
            file = event.file
            type = event.type
        })
    }
}

/**
 * Reports a compilation error if the given entity state [type] is nested in another message.
 *
 * The error refers to the declaration of the [type] because the [option] has no
 * coordinates when its value is set via a field, as in `option (entity).kind = AGGREGATE`.
 *
 * @param type The type of the entity state.
 * @param option The option which makes the [type] an entity state.
 * @param file The file declaring the [type].
 */
private fun checkTopLevel(type: MessageType, option: Option, file: File) =
    Compilation.check(type.isTopLevel, file, type.span) {
        "The `(${option.name})` option cannot be applied to the message `${type.qualifiedName}`" +
                " because it is nested in `${type.declaredIn.qualifiedName}`." +
                " An entity state must be a top-level message." +
                " Please move `${type.name.simpleName}` to the top level of the file."
    }
