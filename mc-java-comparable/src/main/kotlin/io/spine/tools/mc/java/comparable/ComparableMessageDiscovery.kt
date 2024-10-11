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

package io.spine.tools.mc.java.comparable

import io.spine.core.External
import io.spine.option.CompareByOption
import io.spine.protobuf.AnyPacker.unpack
import io.spine.protodata.ast.event.TypeDiscovered
import io.spine.protodata.plugin.Policy
import io.spine.protodata.settings.loadSettings
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import io.spine.server.tuple.EitherOf2
import io.spine.server.tuple.EitherOf2.withA
import io.spine.server.tuple.EitherOf2.withB
import io.spine.tools.mc.java.comparable.event.ComparableMessageDiscovered
import io.spine.tools.mc.java.comparable.event.comparableMessageDiscovered
import io.spine.tools.mc.java.settings.Comparables

/**
 * Discovers comparable messages.
 */
internal class ComparableMessageDiscovery : Policy<TypeDiscovered>(), ComparableComponent {

    private val settings: Comparables by lazy { loadSettings() }

    @React
    override fun whenever(
        @External event: TypeDiscovered
    ): EitherOf2<ComparableMessageDiscovered, NoReaction> {
        val options = event.type.optionList
        val compareBy = options.find(::isCompareBy)
        return compareBy?.let {
            withA(
                comparableMessageDiscovered {
                    type = event.type
                    option = unpack(compareBy.value, CompareByOption::class.java)
                    actions = settings.actions
                }
            )
        } ?: withB(noReaction())
    }
}
