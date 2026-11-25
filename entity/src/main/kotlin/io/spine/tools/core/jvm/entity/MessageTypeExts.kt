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

import io.spine.tools.compiler.ast.Field
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.core.jvm.settings.Entities

/**
 * Tells if this message type is an entity state, according to the given settings.
 */
internal fun MessageType.isEntityState(settings: Entities): Boolean {
    val typeOptions = optionList.map { it.name }
    val options = settings.optionList.map { it.name }
    val result = typeOptions.any { it in options }
    return result
}

/**
 * Obtains the ID field of this message type, which is the first in the declaration order.
 *
 * @param settings
 *         code generation settings for the entity states.
 * @throws IllegalStateException
 *          if this type is not an entity state, according to the given settings.
 *          Or, if there are no fields declared in this type.
 * @return the ID field.
 */
internal fun MessageType.idField(settings: Entities): Field {
    check(isEntityState(settings)) {
        "The type `$qualifiedName` is not an entity state."
    }
    check(fieldList.isNotEmpty()) {
        "The entity state `$qualifiedName` must have at least one field."
    }
    return fieldList.first()
}
