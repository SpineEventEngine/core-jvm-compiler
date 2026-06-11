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

package io.spine.tools.core.annotation.given

import com.google.protobuf.BoolValue
import io.spine.protobuf.pack
import io.spine.tools.compiler.ast.Field
import io.spine.tools.compiler.ast.Option
import io.spine.tools.compiler.ast.PrimitiveType
import io.spine.tools.compiler.ast.ServiceName
import io.spine.tools.compiler.ast.TypeInstances
import io.spine.tools.compiler.ast.TypeName
import io.spine.tools.compiler.ast.field
import io.spine.tools.compiler.ast.fieldName
import io.spine.tools.compiler.ast.fieldType
import io.spine.tools.compiler.ast.file
import io.spine.tools.compiler.ast.option
import io.spine.tools.compiler.ast.serviceName
import io.spine.tools.compiler.ast.typeName
import io.spine.tools.core.annotation.event.FileOptionMatched
import io.spine.tools.core.annotation.event.fileOptionMatched

/**
 * Creates a boolean option with the given name and the `true` value.
 */
internal fun apiOption(name: String): Option = option {
    this.name = name
    type = TypeInstances.boolean
    value = BoolValue.of(true).pack()
}

/**
 * Creates a `string` field with the given name declared in the given type.
 */
internal fun stringField(name: String, declaredIn: TypeName): Field = field {
    this.name = fieldName { value = name }
    declaringType = declaredIn
    type = fieldType { primitive = PrimitiveType.TYPE_STRING }
}

/**
 * The proto file used by the test events.
 */
internal val testFile = file {
    path = "given/annotation/test_types.proto"
}

/**
 * The name of a message type used by the test events.
 */
internal val messageName: TypeName = typeName {
    packageName = "given.annotation"
    simpleName = "TestMessage"
}

/**
 * The name of an enum type used by the test events.
 */
internal val enumName: TypeName = typeName {
    packageName = "given.annotation"
    simpleName = "TestEnum"
}

/**
 * The name of a service used by the test events.
 */
internal val serviceTestName: ServiceName = serviceName {
    packageName = "given.annotation"
    simpleName = "TestService"
}

/**
 * Creates a [FileOptionMatched] event with the message type as the target.
 */
internal fun fileOptionMatchedWithMessage(): FileOptionMatched = fileOptionMatched {
    file = testFile
    fileOption = apiOption("internal_all")
    messageType = messageName
    assumed = apiOption("internal_type")
}

/**
 * Creates a [FileOptionMatched] event with the enum type as the target.
 */
internal fun fileOptionMatchedWithEnum(): FileOptionMatched = fileOptionMatched {
    file = testFile
    fileOption = apiOption("internal_all")
    enumType = enumName
    assumed = apiOption("internal_type")
}

/**
 * Creates a [FileOptionMatched] event with the service as the target.
 */
internal fun fileOptionMatchedWithService(): FileOptionMatched = fileOptionMatched {
    file = testFile
    fileOption = apiOption("SPI_all")
    service = serviceTestName
    assumed = apiOption("SPI_service")
}
