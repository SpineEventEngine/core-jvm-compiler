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
