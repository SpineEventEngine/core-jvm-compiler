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

import com.google.common.annotations.VisibleForTesting
import io.spine.core.External
import io.spine.protodata.ast.MessageType
import io.spine.protodata.ast.event.TypeDiscovered
import io.spine.protodata.ast.matches
import io.spine.protodata.plugin.Policy
import io.spine.protodata.settings.loadSettings
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import io.spine.server.tuple.EitherOf4
import io.spine.tools.mc.java.settings.SignalSettings
import io.spine.tools.mc.java.settings.Signals
import io.spine.tools.mc.java.signal.event.CommandDiscovered
import io.spine.tools.mc.java.signal.event.EventDiscovered
import io.spine.tools.mc.java.signal.event.RejectionDiscovered
import io.spine.tools.mc.java.signal.event.commandDiscovered
import io.spine.tools.mc.java.signal.event.eventDiscovered
import io.spine.tools.mc.java.signal.event.rejectionDiscovered

/**
 * Reacts to the [TypeDiscovered] event finding out if the discovered type is one
 * of the signals.
 *
 * Uses file patterns defined in [SignalSettings] to distinguish commands, events, or rejections.
 * [CommandDiscovered], [EventDiscovered], or [RejectionDiscovered] events are emitted accordingly.
 * If the discovered type is not a signal, the policy emits [NoReaction].
 *
 * @see CommandsPerFile
 * @see EventsPerFile
 * @see RejectionsPerFile
 */
internal class SignalDiscovery : Policy<TypeDiscovered>(), SignalPluginComponent {

    private val settings: SignalSettings by lazy {
        loadSettings()
    }
    private val commands: Signals by lazy { settings.commands }
    private val events: Signals by lazy { settings.events }
    private val rejections: Signals by lazy { settings.rejections }

    @React
    override fun whenever(@External event: TypeDiscovered):
            EitherOf4<CommandDiscovered, EventDiscovered, RejectionDiscovered, NoReaction> {
        val msg = event.type
        if (msg.isNested) {
            // Signals are only top level messages. Ignore nested types.
            return EitherOf4.withD(noReaction())
        }
        return if (commands.match(msg)) {
            EitherOf4.withA(commandDiscovered {
                file = event.file
                type = msg
            })
        } else if (events.match(msg)) {
            EitherOf4.withB(eventDiscovered {
                file = event.file
                type = msg
            })
        } else if (rejections.match(msg)) {
            EitherOf4.withC(rejectionDiscovered {
                file = event.file
                type = msg
            })
        } else {
            EitherOf4.withD(noReaction())
        }
    }
}

/**
 * Tells if the given message type matches one of the file patterns.
 */
@VisibleForTesting
internal fun Signals.match(type: MessageType): Boolean =
    patternList.any {
        it.matches(type)
    }

private val MessageType.isNested: Boolean
    get () = name.nestingTypeNameCount > 0
