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

package io.spine.tools.core.jvm.marker

import io.spine.core.External
import io.spine.option.EveryIsOption
import io.spine.protodata.ast.event.FileEntered
import io.spine.protodata.ast.find
import io.spine.protodata.plugin.Policy
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import io.spine.server.event.asA
import io.spine.server.event.asB
import io.spine.server.tuple.EitherOf2
import io.spine.tools.core.jvm.marker.event.EveryIsOptionDiscovered
import io.spine.tools.core.jvm.marker.event.everyIsOptionDiscovered

/**
 * Finds files with `(every_is)` option emitting [EveryIsOptionDiscovered], if found.
 */
internal class EveryIsOptionDiscovery : Policy<FileEntered>() {

    @React
    override fun whenever(
        @External event: FileEntered
    ): EitherOf2<EveryIsOptionDiscovered, NoReaction> {
        val found = event.header.optionList.find<EveryIsOption>()
        return if (found != null) {
            everyIsOptionDiscovered {
                file = event.file
                option = found
                header = event.header
            }.asA()
        } else {
            noReaction().asB()
        }
    }
}
