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

package io.spine.tools.mc.java.settings

import com.google.protobuf.Any
import com.google.protobuf.Message
import io.spine.protobuf.pack
import io.spine.protodata.render.Actions
import org.checkerframework.checker.signature.qual.FqBinaryName

/**
 * A fully qualified binary Java class name.
 */
public typealias BinaryClassName = @FqBinaryName String

/**
 * Maps qualified render action class names to parameters specified via Gradle settings.
 */
public typealias ActionMap = Map<BinaryClassName, Message>

/**
 * The shortcut for map entry value telling that an action has no parameter.
 *
 * @see Actions.getActionMap
 */
public val noParameter: Message = Any.getDefaultInstance()

/**
 * Transforms this action map into the map suitable when creating [Actions] by
 * packing values if they are not already instances of [Any].
 */
public fun ActionMap.pack(): Map<BinaryClassName, Any> {
    return map { (key, value) ->
        key to if (value is Any) value else value.pack()
    }.toMap()
}
