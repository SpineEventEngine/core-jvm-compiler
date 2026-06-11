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

package io.spine.tools.core.jvm.settings

import com.google.protobuf.Any
import com.google.protobuf.StringValue
import com.google.protobuf.stringValue
import io.kotest.matchers.shouldBe
import io.spine.protobuf.pack
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Extensions for `ActionMap` should")
internal class ActionsExtsSpec {

    @Test
    fun `provide the default 'Any' as the no-parameter marker`() {
        noParameter shouldBe Any.getDefaultInstance()
    }

    @Test
    fun `keep values which are already packed`() {
        val packed = stringValue { value = "already packed" }.pack()
        val map: ActionMap = mapOf("custom.Action" to packed)

        map.pack() shouldBe mapOf("custom.Action" to packed)
    }

    @Test
    fun `pack message values`() {
        val raw = stringValue { value = "raw" }
        val map: ActionMap = mapOf("custom.Action" to raw)

        val packed = map.pack()
        packed.getValue("custom.Action").unpack(StringValue::class.java) shouldBe raw
    }
}
