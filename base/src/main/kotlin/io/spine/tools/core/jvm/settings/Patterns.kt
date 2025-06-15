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

@file:JvmName("Patterns")

package io.spine.tools.core.jvm.settings

import io.spine.protodata.ast.MessageType
import io.spine.protodata.ast.matches
import io.spine.tools.core.jvm.settings.Pattern.KindCase.FILE
import io.spine.tools.core.jvm.settings.Pattern.KindCase.TYPE
import io.spine.tools.core.jvm.settings.TypePattern.ValueCase.EXPECTED_TYPE
import io.spine.tools.core.jvm.settings.TypePattern.ValueCase.REGEX

/**
 * Tells if this pattern matches the given [type].
 *
 * @see io.spine.protodata.ast.FilePattern.matches
 * @see TypePattern.matches
 */
public fun Pattern.matches(type: MessageType): Boolean {
    return when (kindCase) {
        FILE -> file.matches(type.file)
        TYPE -> this@matches.type.matches(type)
        else -> false
    }
}

/**
 * Tells if this type pattern matches the given type.
 *
 * @see Pattern.matches
 * @see io.spine.protodata.ast.FilePattern.matches
 */
public fun TypePattern.matches(type: MessageType): Boolean {
    val qualifiedName = type.qualifiedName
    return when (valueCase) {
        EXPECTED_TYPE -> expectedType.value == qualifiedName
        REGEX -> Regex(regex).matches(qualifiedName)
        else -> false
    }
}
