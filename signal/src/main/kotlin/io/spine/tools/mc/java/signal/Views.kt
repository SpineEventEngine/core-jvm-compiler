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

package io.spine.tools.mc.java.signal

import io.spine.core.Subscribe
import io.spine.protodata.ast.File
import io.spine.protodata.plugin.View
import io.spine.server.entity.alter
import io.spine.tools.mc.java.signal.event.CommandDiscovered
import io.spine.tools.mc.java.signal.event.EventDiscovered
import io.spine.tools.mc.java.signal.event.RejectionDiscovered

/**
 * Collects command types discovered in the Protobuf files passed to the Signal Plugin.
 *
 * @see [io.spine.base.CommandMessage]
 */
internal class CommandsPerFile : View<File, CommandActions, CommandActions.Builder>() {

    @Subscribe
    fun on(e: CommandDiscovered) = alter {
        addType(e.type)
    }
}

/**
 * Collects event types discovered in the Protobuf files passed to the Signal Plugin.
 *
 * @see [io.spine.base.EventMessage]
 */
internal class EventsPerFile : View<File, EventActions, EventActions.Builder>() {

    @Subscribe
    fun on(e: EventDiscovered) = alter {
        addType(e.type)
    }
}

/**
 * Collects rejection types discovered in the Protobuf files passed to the Signal Plugin.
 *
 * @see [io.spine.base.RejectionMessage]
 */
internal class RejectionsPerFile : View<File, RejectionActions, RejectionActions.Builder>() {

    @Subscribe
    fun on(e: RejectionDiscovered) = alter {
        addType(e.type)
    }
}
