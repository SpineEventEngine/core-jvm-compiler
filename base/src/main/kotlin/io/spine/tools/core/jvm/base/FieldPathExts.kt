/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.tools.mc.java.base

import io.spine.base.FieldPath
import io.spine.base.fieldPath
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.MessageType
import io.spine.protodata.ast.field
import io.spine.protodata.type.TypeSystem

/**
 * Tells if this [FieldPath] doesn't denote a nested field.
 */
public val FieldPath.isNotNested: Boolean
    get() = fieldNameList.size == 1

/**
 * Returns this [FieldPath] as a single [String], where the field names
 * are separated with a dot.
 *
 * For example, `citizen.passport.firstName`.
 */
public val FieldPath.joined: String
    get() = fieldNameList.joinToString(".")

/**
 * Returns the root field's name of this [FieldPath].
 *
 * @throws [NoSuchElementException] if the path is empty.
 */
public val FieldPath.root: String
    get() = fieldNameList.first()

/**
 * Resolves the given [FieldPath] against the given [MessageType] within
 * this [TypeSystem].
 *
 * This method navigates through the nested messages and fields as specified by
 * the [fieldPath], returning the final [Field] that the path points to.
 *
 * @param fieldPath The field path to resolve.
 * @param message The message where the root of the [fieldPath] is declared.
 */
public fun TypeSystem.resolve(fieldPath: FieldPath, message: MessageType): Field {
    val currentField = message.field(fieldPath.root)
    if (fieldPath.isNotNested) {
        return currentField
    }

    check(currentField.type.isMessage) {
        "Can't resolve the field path `$fieldPath` because `${currentField.name}` segment " +
                "doesn't denote a message. The type of this field is `${currentField.type}`. " +
                "Only messages can have nested field."
    }

    val currentFieldMessage = currentField.type.message
    val remainingFields = fieldPath.fieldNameList.drop(1)
    val remainingPath = fieldPath { fieldName.addAll(remainingFields) }
    val nextMessageInfo = findMessage(currentFieldMessage)

    check(nextMessageInfo != null) {
        "`$currentFieldMessage` was not found in the passed Proto files or their dependencies."
    }

    val nextMessage = nextMessageInfo.first
    return resolve(remainingPath, nextMessage)
}
