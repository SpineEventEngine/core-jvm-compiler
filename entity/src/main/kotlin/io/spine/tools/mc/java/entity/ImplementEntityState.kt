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

package io.spine.tools.mc.java.entity

import com.google.protobuf.Empty
import io.spine.base.EntityState
import io.spine.protodata.ast.MessageType
import io.spine.protodata.ast.firstField
import io.spine.protodata.context.CodegenContext
import io.spine.protodata.java.javaType
import io.spine.protodata.java.render.DirectMessageAction
import io.spine.protodata.java.render.ImplementInterface
import io.spine.protodata.java.render.superInterface
import io.spine.protodata.render.SourceFile
import io.spine.tools.code.Java
import io.spine.tools.java.reference

/**
 * Updates the Java code of a message type which qualifies as [EntityState] by
 * making it implement this interface.
 *
 * The class is public because its fully qualified name is used as a default
 * value in [EntitySettings][io.spine.tools.mc.java.gradle.settings.EntitySettings].
 *
 * ## Implementation note
 *
 * The class descends from [DirectMessageAction] and delegates to [ImplementInterface] in
 * the [doRender] method instead of extending [ImplementInterface] directly.
 * This is so because of the following.
 * The resolution of the ID field type requires an instance of
 * [TypeSystem][io.spine.protodata.type.TypeSystem].
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
        val action = ImplementInterface(
            type,
            file,
            superInterface {
                name = EntityState::class.java.reference
                genericArgument.add(idFieldType)
            },
            context
        )
        action.render()
    }
}
