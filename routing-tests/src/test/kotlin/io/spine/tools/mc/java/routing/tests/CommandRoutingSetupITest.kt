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

package io.spine.tools.mc.java.routing.tests

import io.spine.given.home.Device
import io.spine.given.home.DeviceAggregate
import io.spine.given.home.DeviceId
import io.kotest.matchers.shouldBe
import io.spine.given.home.State
import io.spine.given.home.commands.addDevice
import io.spine.given.home.commands.setState
import io.spine.given.home.homeAutomation
import io.spine.testing.server.blackbox.BlackBox
import io.spine.testing.server.blackbox.assertEntity
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Generated `CommandRoutingSetup` should")
internal class CommandRoutingSetupITest {

    @Test
    fun `apply generated routes`() {
        BlackBox.from(homeAutomation()).use { context ->
            val l1 = DeviceId.generate()

            context.receivesCommand(
                addDevice {
                    device = l1
                    name = "First Lamp"
                }
            )

            // The first command is handled using the standard routing
            // via the first command field.
            var lamp = context.readState(l1)
            lamp.state shouldBe State.OFF

            context.receivesCommand(
                setState {
                    device = l1
                    state = State.ON
                }
            )

            // The command was handled via custom routing declared in the class.
            lamp = context.readState(l1)
            lamp.state shouldBe State.ON
        }
    }
}

/**
 * Reads the state of the [DeviceAggregate] with the given ID.
 */
private fun BlackBox.readState(id: DeviceId) =
    assertEntity<DeviceAggregate, DeviceId>(id).actual()?.state() as Device
