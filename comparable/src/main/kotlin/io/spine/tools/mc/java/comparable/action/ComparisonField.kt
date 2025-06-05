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

package io.spine.tools.mc.java.comparable.action

import io.spine.base.FieldPath
import io.spine.protodata.ast.MessageType
import io.spine.protodata.ast.PrimitiveType

/**
 * A field that participates in comparison.
 *
 * @property path The field path as was passed to the option.
 */
internal sealed class ComparisonField(val path: FieldPath)

/**
 * An enum field, which participates in comparison.
 *
 * @param path The field path as was passed to the option.
 */
internal class EnumComparisonField(path: FieldPath) : ComparisonField(path)

/**
 * A primitive field, which participates in comparison.
 *
 * @param path The field path as was passed to the option.
 * @param type The field type.
 */
internal class PrimitiveComparisonField(
    path: FieldPath,
    val type: PrimitiveType
) : ComparisonField(path)

/**
 * A message field, which participates in comparison.
 *
 * @param path The field path as was passed to the option.
 * @param type The field type.
 */
internal class MessageComparisonField(
    path: FieldPath,
    val type: MessageType,
) : ComparisonField(path)

/**
 * An external message field, which participates in comparison.
 *
 * An external message is a message, Java code for which is NOT being generated
 * by the ongoing generation request. It is a dependency that is present on
 * the classpath of code generation and thus, has the [clazz] instance.
 *
 * @param path The field path as was passed to the option.
 * @param type The field type.
 * @param clazz The Java class of the field type.
 */
internal class ExternalMessageComparisonField(
    path: FieldPath,
    val type: MessageType,
    val clazz: Class<*>
) : ComparisonField(path)
