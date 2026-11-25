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

import io.spine.tools.compiler.Compilation
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.compiler.ast.Span
import io.spine.tools.compiler.ast.TypeName
import io.spine.tools.compiler.ast.qualifiedName
import io.spine.tools.compiler.check
import io.spine.tools.compiler.type.TypeSystem
import io.spine.tools.compiler.type.enum
import io.spine.tools.compiler.type.message
import io.spine.tools.core.jvm.entity.EntityPlugin.Companion.COLUMN_CLASS_NAME
import io.spine.tools.core.jvm.entity.EntityPlugin.Companion.QUERY_BUILDER_CLASS_NAME
import io.spine.tools.core.jvm.entity.EntityPlugin.Companion.QUERY_CLASS_NAME

/**
 * The names of the classes which [EntityPlugin] nests in the Java class of an entity state.
 */
internal val reservedNames: Set<String> = setOf(
    COLUMN_CLASS_NAME,
    QUERY_CLASS_NAME,
    QUERY_BUILDER_CLASS_NAME
)

/**
 * Checks that an entity state, and the messages and enums nested in it, do not have
 * [reserved names][reservedNames].
 *
 * The classes with the reserved names are nested in the Java class of the entity state.
 * The Java code does not compile if the state itself or a type nested in it has the same name.
 *
 * Types nested deeper than one level may have any names.
 *
 * @param state The entity state to check.
 * @param typeSystem The type system to obtain the nested types.
 */
internal class ReservedNamesCheck(
    private val state: MessageType,
    private val typeSystem: TypeSystem
) {

    /**
     * Reports a compilation error if a reserved name is found.
     */
    fun perform() {
        checkName(type = state.name, kind = MESSAGE, span = state.span)
        state.nestedMessagesList.forEach {
            checkName(type = it, kind = MESSAGE, span = typeSystem.message(it).span)
        }
        state.nestedEnumsList.forEach {
            checkName(type = it, kind = ENUM, span = typeSystem.enum(it).span)
        }
    }

    private fun checkName(type: TypeName, kind: String, span: Span) {
        val name = type.simpleName
        Compilation.check(name !in reservedNames, state.file, span) {
            "The name of the $kind `${type.qualifiedName}` is reserved." +
                    " The compiler generates the class `$name`" +
                    " for the entity state `${state.qualifiedName}`." +
                    " Please rename the $kind."
        }
    }

    private companion object {
        const val MESSAGE = "message"
        const val ENUM = "enum"
    }
}
