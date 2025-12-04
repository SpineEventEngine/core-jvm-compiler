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

package io.spine.tools.core.jvm.mgroup

import io.spine.core.External
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import io.spine.server.event.asA
import io.spine.server.event.asB
import io.spine.server.tuple.EitherOf2
import io.spine.tools.compiler.ast.event.TypeDiscovered
import io.spine.tools.compiler.ast.isTopLevel
import io.spine.tools.compiler.plugin.Reaction
import io.spine.tools.compiler.settings.loadSettings
import io.spine.tools.core.jvm.mgroup.event.GroupedMessageDiscovered
import io.spine.tools.core.jvm.mgroup.event.groupedMessageDiscovered
import io.spine.tools.core.jvm.settings.GroupSettings
import io.spine.tools.core.jvm.settings.matches

/**
 * Detects message types matching [GroupSettings] in response to [TypeDiscovered] event.
 *
 * If the type matches one or more groups emits [GroupedMessageDiscovered] event.
 */
internal class GroupedMessageDiscovery : Reaction<TypeDiscovered>(), MessageGroupPluginComponent {

    private val settings: GroupSettings by lazy {
        loadSettings()
    }

    @React
    override fun whenever(
        @External event: TypeDiscovered
    ): EitherOf2<GroupedMessageDiscovered, NoReaction> {
        val type = event.type
        val matchingGroups = settings.groupList.filter {
            it.pattern.matches(type) && type.isTopLevel
        }
        return if (matchingGroups.isNotEmpty()) {
            groupedMessageDiscovered {
                this@groupedMessageDiscovered.type = type
                group.addAll(matchingGroups)
            }.asA()
        } else {
            noReaction().asB()
        }
    }
}
