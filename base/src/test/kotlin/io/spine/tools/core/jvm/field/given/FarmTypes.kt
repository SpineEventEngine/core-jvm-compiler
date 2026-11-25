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

package io.spine.tools.core.jvm.field.given

import io.spine.tools.compiler.jvm.ClassName
import io.spine.tools.compiler.protobuf.ProtoFileList
import io.spine.tools.compiler.protobuf.toMessageType
import io.spine.tools.compiler.protobuf.toPbSourceFile
import io.spine.tools.compiler.type.TypeSystem
import io.spine.tools.core.jvm.field.AddFieldClass
import io.spine.tools.core.jvm.field.FieldClassNames
import io.spine.tools.core.jvm.given.base.Barn
import io.spine.tools.core.jvm.given.base.Farm

/**
 * Creates a [TypeSystem] that knows only the types declared in `farm.proto`.
 */
internal fun farmTypeSystem(): TypeSystem =
    TypeSystem(
        ProtoFileList(emptyList()),
        setOf(Farm.getDescriptor().file.toPbSourceFile())
    )

/**
 * Creates the names of the classes generated for the types of the fields of [Farm] that
 * are declared in `farm.proto`.
 */
internal fun farmFieldClassNames(): FieldClassNames {
    val fieldTypes = listOf(Barn.getDescriptor(), Barn.Stall.getDescriptor())
    return FieldClassNames(
        messageType = Farm.getDescriptor().toMessageType(),
        fieldClass = ClassName(Farm::class.java).nested(AddFieldClass.NAME),
        fieldTypes = fieldTypes.map { it.toMessageType().name }.toSet()
    )
}
