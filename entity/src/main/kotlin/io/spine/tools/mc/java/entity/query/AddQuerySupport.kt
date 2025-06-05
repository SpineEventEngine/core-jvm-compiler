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

package io.spine.tools.mc.java.entity.query

import com.google.protobuf.Empty
import io.spine.protodata.ast.MessageType
import io.spine.protodata.context.CodegenContext
import io.spine.protodata.java.render.DirectMessageAction
import io.spine.protodata.render.SourceFile
import io.spine.protodata.settings.loadSettings
import io.spine.tools.code.Java
import io.spine.tools.mc.java.entity.EntityPluginComponent
import io.spine.tools.mc.java.settings.Entities
import io.spine.tools.psi.java.execute

/**
 * Extends the code of an entity state type for supporting queries.
 *
 * In particular:
 *  * Adds a nested class called [QueryBuilder][QueryBuilderClass].
 *  * Adds a nested class called [Query][QueryClass].
 *  * Adds the method called [query][QueryMethod] under the entity state class.
 */
public class AddQuerySupport(
    type: MessageType,
    file: SourceFile<Java>,
    context: CodegenContext
) : DirectMessageAction<Empty>(type, file, Empty.getDefaultInstance(), context),
    EntityPluginComponent {

    private val settings: Entities by lazy {
        loadSettings()
    }

    override fun doRender() {
        execute {
            // The `query()` method is added after constructors.
            QueryMethod(file).run {
                render()
            }
            // The `QueryBuilder` class is added at the bottom, before the `Query` class.
            QueryBuilderClass(type, file, settings, context).run {
                render()
            }
            // The `Query` class comes last.
            QueryClass(type, file, settings, context).run {
                render()
            }
        }
    }
}
