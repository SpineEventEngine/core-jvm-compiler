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
