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

import com.google.protobuf.BoolValue
import com.google.protobuf.DoubleValue
import com.google.protobuf.FloatValue
import com.google.protobuf.Int32Value
import com.google.protobuf.Int64Value
import com.google.protobuf.StringValue
import com.google.protobuf.UInt32Value
import com.google.protobuf.UInt64Value

/**
 * Enumerates well-known Protobuf value messages, which are allowed to be used
 * in `compare_by` option.
 *
 * These messages don't require a specific comparator as they consist of a single
 * primitive `value` field. This field should be used for comparisons.
 *
 * See also: [Protobuf Docs | Well-Known Types](https://protobuf.dev/reference/protobuf/google.protobuf/).
 */
internal object ProtoValueMessages {

    private val valueClasses = setOf(
        BoolValue::class,
        DoubleValue::class,
        FloatValue::class,
        Int32Value::class,
        Int64Value::class,
        UInt32Value::class,
        UInt64Value::class,
        StringValue::class,
    ).map { it.java }

    /**
     * Tells if this [Class] denotes a well-known Protobuf value message.
     */
    val Class<*>.isProtoValueMessage
        get() = valueClasses.contains(this)
}
