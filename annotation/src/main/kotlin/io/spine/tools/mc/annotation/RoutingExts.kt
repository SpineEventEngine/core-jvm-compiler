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

package io.spine.tools.mc.annotation

import io.spine.protodata.ast.ServiceName
import io.spine.protodata.ast.TypeName
import io.spine.tools.mc.annotation.event.FileOptionMatched
import io.spine.tools.mc.annotation.event.FileOptionMatched.TargetCase.ENUM_TYPE
import io.spine.tools.mc.annotation.event.FileOptionMatched.TargetCase.MESSAGE_TYPE
import io.spine.tools.mc.annotation.event.FileOptionMatched.TargetCase.SERVICE

/**
 * A routing function which obtains a single-item set for the target entity,
 * IFF the target is a `messageType`. Otherwise, returns an empty set.
 */
internal fun FileOptionMatched.toMessageTypeName(): Set<TypeName> =
    if (targetCase == MESSAGE_TYPE) {
        setOf(messageType)
    } else {
        setOf()
    }

/**
 * A routing function which obtains a single-item set for the target entity,
 * IFF the target is a `enumType`. Otherwise, returns an empty set.
 */
internal fun FileOptionMatched.toEnumTypeName(): Set<TypeName> =
    if (targetCase == ENUM_TYPE) {
        setOf(enumType)
    } else {
        setOf()
    }

/**
 * A routing function which obtains a single-item set for the target entity,
 * IFF the target is a `service`. Otherwise, returns an empty set.
 */
internal fun FileOptionMatched.toServiceName(): Set<ServiceName> =
    if (targetCase == SERVICE) {
        setOf(service)
    } else {
        setOf()
    }
