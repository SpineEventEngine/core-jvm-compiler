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

package io.spine.tools.core.annotation

import com.google.protobuf.BoolValue
import com.google.protobuf.StringValue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.spine.protobuf.pack
import io.spine.tools.core.annotation.given.apiOption
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`ApiOption` should")
internal class ApiOptionSpec {

    @Test
    fun `declare file and message options for all API levels`() {
        ApiOption.BETA.fileOption.name shouldBe "beta_all"
        ApiOption.BETA.messageOption.name shouldBe "beta_type"
        ApiOption.BETA.fieldOption?.name shouldBe "beta"

        ApiOption.EXPERIMENTAL.fileOption.name shouldBe "experimental_all"
        ApiOption.EXPERIMENTAL.messageOption.name shouldBe "experimental_type"
        ApiOption.EXPERIMENTAL.fieldOption?.name shouldBe "experimental"

        ApiOption.INTERNAL.fileOption.name shouldBe "internal_all"
        ApiOption.INTERNAL.messageOption.name shouldBe "internal_type"
        ApiOption.INTERNAL.fieldOption?.name shouldBe "internal"

        ApiOption.SPI.fileOption.name shouldBe "SPI_all"
        ApiOption.SPI.messageOption.name shouldBe "SPI_type"
        ApiOption.SPI.serviceOption?.name shouldBe "SPI_service"
    }

    @Test
    fun `not declare options which do not exist at a level`() {
        ApiOption.SPI.fieldOption.shouldBeNull()
        ApiOption.BETA.serviceOption.shouldBeNull()
        ApiOption.EXPERIMENTAL.serviceOption.shouldBeNull()
        ApiOption.INTERNAL.serviceOption.shouldBeNull()
    }

    @Nested inner class
    `find a matching option` {

        private fun byName(name: String) = ApiOption.findMatching(apiOption(name))

        @Test
        fun `by a file-level option name`() {
            byName("beta_all") shouldBe ApiOption.BETA
            byName("internal_all") shouldBe ApiOption.INTERNAL
        }

        @Test
        fun `by a message-level option name`() {
            byName("experimental_type") shouldBe ApiOption.EXPERIMENTAL
            byName("SPI_type") shouldBe ApiOption.SPI
        }

        @Test
        fun `by a field-level option name`() {
            byName("beta") shouldBe ApiOption.BETA
            byName("experimental") shouldBe ApiOption.EXPERIMENTAL
            byName("internal") shouldBe ApiOption.INTERNAL
        }

        @Test
        fun `by a service-level option name`() {
            byName("SPI_service") shouldBe ApiOption.SPI
        }

        @Test
        fun `returning 'null' for a non-API option`() {
            byName("deprecated").shouldBeNull()
        }
    }

    @Nested inner class
    `tell if an option value is 'true'` {

        @Test
        fun `for a packed 'true' value`() {
            BoolValue.of(true).pack().isTrue().shouldBeTrue()
        }

        @Test
        fun `for a packed 'false' value`() {
            BoolValue.of(false).pack().isTrue().shouldBeFalse()
        }

        @Test
        fun `for a non-boolean value`() {
            StringValue.of("yes").pack().isTrue().shouldBeFalse()
        }
    }
}
