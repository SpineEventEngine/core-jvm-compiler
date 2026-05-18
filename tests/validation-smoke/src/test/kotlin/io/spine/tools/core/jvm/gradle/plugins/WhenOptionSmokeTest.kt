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

package io.spine.tools.core.jvm.gradle.plugins

import com.google.protobuf.Duration
import com.google.protobuf.Timestamp
import com.google.protobuf.util.Durations.fromMillis
import com.google.protobuf.util.Timestamps
import io.spine.test.tools.validate.futureSpineTemporal
import io.spine.test.tools.validate.futureTimestamp
import io.spine.test.tools.validate.pastSpineTemporal
import io.spine.test.tools.validate.pastTimestamp
import io.spine.time.LocalDateTimes
import io.spine.validation.ValidationException
import java.time.Instant
import java.time.LocalDateTime.ofInstant
import java.time.ZoneOffset.UTC
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import io.spine.time.LocalDateTime as SpineTimeLocalDateTime

@DisplayName("`(when)` constraint should")
@Disabled("Until Spine Time migrates to new `TemplateString` API.")
internal class WhenOptionSmokeTest {

    @Nested
    @DisplayName("if used with Protobuf `Timestamp` and given a timestamp denoting")
    inner class TimestampTest {

        @Nested inner class
        `the past` {

            @Test
            fun `throw, if restricted to be in future`() {
                assertThrows<ValidationException> {
                    futureTimestamp {
                        value = pastTimestamp()
                    }
                }
            }

            @Test
            fun `pass, if restricted to be in past`() {
                assertDoesNotThrow {
                    pastTimestamp {
                        value = pastTimestamp()
                    }
                }
            }
        }

        @Nested inner class
        `the future` {

            @Test
            fun `throw, if restricted to be in past`() {
                assertThrows<ValidationException> {
                    pastTimestamp {
                        value = futureTimestamp()
                    }
                }
            }

            @Test
            fun `pass, if restricted to be in future`() {
                assertDoesNotThrow {
                    futureTimestamp {
                        value = futureTimestamp()
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("if used with Spine `Temporal` and given a temporal denoting")
    inner class SpineTemporalTest {

        @Nested inner class
        `the past` {

            @Test
            fun `throw, if restricted to be in future`() {
                assertThrows<ValidationException> {
                    futureSpineTemporal {
                        value = pastSpineTime()
                    }
                }
            }

            @Test
            fun `pass, if restricted to be in past`() {
                assertDoesNotThrow {
                    pastSpineTemporal {
                        value = pastSpineTime()
                    }
                }
            }
        }

        @Nested inner class
        `the future` {

            @Test
            fun `throw, if restricted to be in past`() {
                assertThrows<ValidationException> {
                    pastSpineTemporal {
                        value = futureSpineTime()
                    }
                }
            }

            @Test
            fun `pass, if restricted to be in future`() {
                assertDoesNotThrow {
                    futureSpineTemporal {
                        value = futureSpineTime()
                    }
                }
            }
        }
    }
}

private fun pastTimestamp(): Timestamp {
    val current = Timestamps.now()
    val past = Timestamps.subtract(current, HALF_OF_SECOND_PROTO)
    return past
}

private fun futureTimestamp(): Timestamp {
    val current = Timestamps.now()
    val future = Timestamps.add(current, HALF_OF_SECOND_PROTO)
    return future
}

private fun pastSpineTime(): SpineTimeLocalDateTime {
    val current = Instant.now()
    val past = current.minusMillis(HALF_OF_SECOND)
    return LocalDateTimes.of(ofInstant(past, UTC))
}

private fun futureSpineTime(): SpineTimeLocalDateTime {
    val current = Instant.now()
    val future = current.plusMillis(HALF_OF_SECOND)
    return LocalDateTimes.of(ofInstant(future, UTC))
}

private val HALF_OF_SECOND_PROTO: Duration = fromMillis(500)
private const val HALF_OF_SECOND: Long = 500
