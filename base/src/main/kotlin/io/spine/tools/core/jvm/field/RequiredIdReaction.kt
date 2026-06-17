/*
 * Copyright 2026, TeamDev. All rights reserved.
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

package io.spine.tools.core.jvm.field

import io.spine.annotation.VisibleForTesting
import io.spine.option.OptionsProto
import io.spine.server.event.NoReaction
import io.spine.server.event.asA
import io.spine.server.tuple.EitherOf2
import io.spine.tools.compiler.Compilation
import io.spine.tools.compiler.ast.Field
import io.spine.tools.compiler.ast.FieldType
import io.spine.tools.compiler.ast.File
import io.spine.tools.compiler.ast.PrimitiveType
import io.spine.tools.compiler.ast.TypeName
import io.spine.tools.compiler.ast.event.TypeDiscovered
import io.spine.tools.compiler.ast.findOption
import io.spine.tools.compiler.ast.name
import io.spine.tools.compiler.ast.qualifiedName
import io.spine.tools.compiler.ast.ref
import io.spine.tools.compiler.check
import io.spine.tools.compiler.plugin.Reaction
import io.spine.tools.validation.event.RequiredFieldDiscovered
import io.spine.tools.validation.event.requiredFieldDiscovered
import io.spine.tools.validation.option.required.RequiredFieldSupport.isSupported

/**
 * An abstract base for reactions that control whether an ID field
 * should be implicitly validated as required.
 *
 * The ID of a signal message or an entity state is the first field
 * declared in the type, disregarding the index of the proto field.
 *
 * The ID field is assumed as required for commands and entity states,
 * unless it is specifically marked otherwise using the field options.
 *
 * Implementations define the ways of discovering signal and entity
 * state messages.
 */
public abstract class RequiredIdReaction : Reaction<TypeDiscovered>() {

    /**
     * Controls whether the given ID [field] should be implicitly validated
     * as required.
     *
     * The method first checks that the [field] has a type supported for an ID:
     * a `string`, a 32-bit or 64-bit integer, or a `Message`. A `repeated` field,
     * a `map`, or a field of any other type (such as `bool`, `float`, `double`,
     * `bytes`, or an `enum`) is rejected with a compilation error before the
     * conditions below are evaluated.
     * See `io.spine.base.Identifier` for the supported identifier types.
     *
     * Then the method emits [RequiredFieldDiscovered] event if the following
     * conditions are met:
     *
     * 1. The field does not have the `(required)` option specified explicitly.
     *   If it has, the field is handled by
     *   `io.spine.tools.validation.option.required.RequiredReaction` of
     *   the Validation Compiler.
     *
     * 2. The field type is not `google.protobuf.Empty`, nor a `repeated` of `Empty`,
     *   nor a `map` with `Empty` values. Otherwise, the method reports a compilation
     *   error because such an implicitly required field can never be satisfied.
     *
     * 3. The field type is supported by the option.
     *
     * The method emits [NoReaction] in case of violation of conditions (1) or (3).
     *
     * @param field The ID field.
     * @param file The file declaring the type which owns the [field].
     * @param message The error message for the violation.
     */
    @Suppress("ReturnCount") // Prefer sooner exit and precise conditions.
    protected fun withField(
        field: Field,
        file: File,
        message: String
    ): EitherOf2<RequiredFieldDiscovered, NoReaction> {
        checkIdTypeSupported(field, file)

        val requiredOption = field.findOption(OptionsProto.required)
        if (requiredOption != null) {
            return ignore()
        }

        checkFieldIsNotEmpty(field, file)

        val fieldTypeUnsupported = field.type.isSupported().not()
        if (fieldTypeUnsupported) {
            return ignore()
        }

        return requiredFieldDiscovered {
            id = field.ref
            defaultErrorMessage = message
            subject = field
        }.asA()
    }
}

/**
 * Reports a compilation error if the type of the given ID [field] is not supported.
 *
 * By convention, an ID field must be a `string`, a 32-bit or 64-bit integer, or
 * a `Message`. A `repeated` field, a `map`, or a field of any other type (such as
 * `bool`, `float`, `double`, `bytes`, or an `enum`) is rejected at compile time.
 *
 * See [io.spine.base.Identifier] for the supported identifier types.
 *
 * @see io.spine.base.Identifier
 */
private fun checkIdTypeSupported(field: Field, file: File) =
    Compilation.check(field.type.isSupportedIdType(), file, field.span) {
        "The ID field `${field.qualifiedName}` of type `${field.type.name}`" +
                " is not supported. An ID field must be a `string`," +
                " a 32-bit or 64-bit integer, or a `Message`." +
                " It cannot be a `bool`, `float`, `double`, `bytes`, `enum`," +
                " `repeated`, or `map` field." +
                " Please change the type of the field."
    }

/**
 * Tells if this [FieldType] is one of the types supported for an ID field:
 * a `string`, a 32-bit or 64-bit integer, or a `Message`.
 *
 * See `io.spine.base.Identifier` for the supported identifier types.
 */
@VisibleForTesting
internal fun FieldType.isSupportedIdType(): Boolean = when {
    isMessage -> true
    isPrimitive -> primitive in supportedIdPrimitives
    else -> false
}

/**
 * The primitive types that can be used for an ID field.
 *
 * These are `string` and all 32-bit and 64-bit integer types, which are
 * represented in Java by `Integer` and `Long`, respectively.
 */
private val supportedIdPrimitives: Set<PrimitiveType> = setOf(
    PrimitiveType.TYPE_STRING,
    PrimitiveType.TYPE_INT32,
    PrimitiveType.TYPE_UINT32,
    PrimitiveType.TYPE_SINT32,
    PrimitiveType.TYPE_FIXED32,
    PrimitiveType.TYPE_SFIXED32,
    PrimitiveType.TYPE_INT64,
    PrimitiveType.TYPE_UINT64,
    PrimitiveType.TYPE_SINT64,
    PrimitiveType.TYPE_FIXED64,
    PrimitiveType.TYPE_SFIXED64,
)

/**
 * Reports a compilation error if the type of the given [field] is
 * `google.protobuf.Empty`.
 *
 * An implicitly required ID field of type `Empty` cannot be satisfied at runtime:
 * every `Empty` instance equals the default value, so the generated required-check
 * is always violated. Such a field is therefore rejected at compile time, mirroring
 * the check performed by the Validation Compiler for the explicit `(required)` option.
 * A `repeated` or `map` of `Empty` is already rejected earlier as an unsupported ID type.
 */
private fun checkFieldIsNotEmpty(field: Field, file: File) =
    Compilation.check(!field.type.refersToEmpty(), file, field.span) {
        "The field `${field.qualifiedName}` of type `${field.type.name}` is assumed to be" +
                " `(required)` by convention, but `google.protobuf.Empty` has no fields and" +
                " its instances are always equal to the default value." +
                " Please change the type of the field."
    }

/**
 * Tells if this [FieldType] is `google.protobuf.Empty`.
 */
private fun FieldType.refersToEmpty(): Boolean = isMessage && message.isProtobufEmpty

private val TypeName.isProtobufEmpty: Boolean
    get() = packageName == "google.protobuf" && simpleName == "Empty"
