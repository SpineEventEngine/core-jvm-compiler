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
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.compiler.ast.Option
import io.spine.tools.compiler.ast.event.TypeDiscovered
import io.spine.tools.compiler.ast.isTopLevel
import io.spine.tools.compiler.ast.qualifiedName
import io.spine.tools.compiler.check
import io.spine.tools.compiler.jvm.javaClassName
import io.spine.tools.compiler.plugin.Reaction
import io.spine.tools.compiler.settings.loadSettings
import io.spine.tools.core.jvm.entity.event.EntityStateDiscovered
import io.spine.tools.core.jvm.entity.event.entityStateDiscovered
import io.spine.tools.core.jvm.settings.Entities

/**
 * Reacts to the [TypeDiscovered] and emits [EntityStateDiscovered], if the discovered
 * type is an [EntityState][io.spine.base.EntityState].
 *
 * The reaction checks if the discovered type has one of the options specified in
 * the [Entities.getOptionList] settings passed to the [EntityPlugin].
 * If so, [EntityStateDiscovered] is emitted. Otherwise, no events are produced.
 *
 * The Java class of an entity state must be a top-level class.
 * The reaction reports a compilation error when:
 *
 *  1. The type is nested in another message.
 *  2. The file declaring the type does not set the `java_multiple_files` option to `true`.
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
        checkTopLevel(event.type, option)
        checkMultipleFiles(event.type, option)
        return listOf(entityStateDiscovered {
            name = event.type.name
            file = event.file
            type = event.type
        })
    }

    /**
     * Reports a compilation error if the Java class of the given entity state [type] is nested
     * in the outer class of the file declaring the [type].
     *
     * It happens if the file does not set the `java_multiple_files` option to `true`.
     * The other reason for a Java class to be nested is excluded by [checkTopLevel], which must
     * be called first.
     *
     * The error refers to the declaration of the [type] because there is no source location
     * to point at when the `java_multiple_files` option is absent. The option is `false`
     * by default.
     *
     * @param type The type of the entity state.
     * @param option The option which makes the [type] an entity state.
     */
    private fun checkMultipleFiles(type: MessageType, option: Option) =
        Compilation.check(
            condition = !type.javaClassName(typeSystem).isNested,
            file = type.file,
            span = type.span
        ) {
            "The `(${option.name})` option cannot be applied to" +
                    " the message `${type.qualifiedName}` because the file does not set" +
                    " the `java_multiple_files` option to `true`, so the Java class of" +
                    " the message is nested in the outer class of the file." +
                    " The Java class of an entity state must be a top-level class." +
                    " Please set `java_multiple_files` to `true` in the file."
        }
}

/**
 * Reports a compilation error if the given entity state [type] is nested in another message.
 *
 * The error refers to the declaration of the [type] because the [option] has no
 * source location when its value is set via a field, as in `option (entity).kind = AGGREGATE`.
 *
 * @param type The type of the entity state.
 * @param option The option which makes the [type] an entity state.
 */
private fun checkTopLevel(type: MessageType, option: Option) =
    Compilation.check(condition = type.isTopLevel, file = type.file, span = type.span) {
        "The `(${option.name})` option cannot be applied to the message `${type.qualifiedName}`" +
                " because it is nested in `${type.declaredIn.qualifiedName}`." +
                " An entity state must be a top-level message." +
                " Please move `${type.name.simpleName}` to the top level of the file."
    }
