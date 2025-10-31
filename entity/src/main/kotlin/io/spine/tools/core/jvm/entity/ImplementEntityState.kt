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

import com.google.protobuf.Empty
import io.spine.base.AggregateState
import io.spine.base.EntityState
import io.spine.base.ProcessManagerState
import io.spine.base.ProjectionState
import io.spine.option.EntityOption
import io.spine.option.EntityOption.Kind.AGGREGATE
import io.spine.option.EntityOption.Kind.ENTITY
import io.spine.option.EntityOption.Kind.PROCESS_MANAGER
import io.spine.option.EntityOption.Kind.PROJECTION
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.compiler.ast.firstField
import io.spine.tools.compiler.context.CodegenContext
import io.spine.tools.compiler.jvm.javaType
import io.spine.tools.compiler.jvm.render.DirectMessageAction
import io.spine.tools.compiler.jvm.render.ImplementInterface
import io.spine.tools.compiler.jvm.render.superInterface
import io.spine.tools.compiler.render.SourceFile
import io.spine.tools.code.Java
import io.spine.tools.compiler.ast.option
import io.spine.tools.compiler.ast.unpack
import io.spine.tools.java.reference
import kotlin.reflect.KClass

/**
 * Updates the Java code of a message type which qualifies as [EntityState] by
 * making it implement this interface, or an interface derived from [EntityState].
 *
 * The type of the selected interface is defined by the value of
 * the [kind][EntityOption.Kind] property of [EntityOption].
 *
 * ## API Note
 *
 * The class is public because its fully qualified name is used as a default
 * value in [EntitySettings][io.spine.tools.core.jvm.gradle.settings.EntitySettings].
 *
 * ## Implementation note
 *
 * The class descends from [DirectMessageAction] and delegates to [ImplementInterface] in
 * the [doRender] method instead of extending [ImplementInterface] directly.
 * This is so because of the following.
 * The resolution of the ID field type requires an instance of
 * [TypeSystem][io.spine.tools.compiler.type.TypeSystem].
 * The field type is passed as the generic parameter of the [EntityState] interface.
 * The [typeSystem] property is not yet initialized when the constructor is called.
 * Therefore, we have to use delegation rather than inheritance here.
 *
 * @param type The type of the message.
 * @param file The source code to which the action is applied.
 * @param context The code generation context in which this action runs.
 */
public class ImplementEntityState(
    type: MessageType,
    file: SourceFile<Java>,
    context: CodegenContext
) : DirectMessageAction<Empty>(type, file, Empty.getDefaultInstance(), context) {

    override fun doRender() {
        val idFieldType = type.firstField.javaType(typeSystem)
        val option = type.option<EntityOption>()
        val entityOption = option.unpack<EntityOption>()
        val iface = entityStateInterface(entityOption)
        val action = ImplementInterface(
            type,
            file,
            superInterface {
                name = iface.java.reference
                genericArgument.add(idFieldType)
            },
            context
        )
        action.render()
    }
}

private fun entityStateInterface(option: EntityOption): KClass<out EntityState<*>> {
    return when (option.kind) {
        AGGREGATE -> AggregateState::class
        PROCESS_MANAGER -> ProcessManagerState::class
        PROJECTION -> ProjectionState::class
        ENTITY -> EntityState::class
        else -> error("Unable to convert the entity kind: `${option.kind}`.")
    }
}
