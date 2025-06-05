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

package io.spine.tools.mc.annotation

import io.spine.core.External
import io.spine.core.Subscribe
import io.spine.protodata.ast.TypeName
import io.spine.protodata.ast.event.EnumOptionDiscovered
import io.spine.protodata.plugin.View
import io.spine.protodata.plugin.ViewRepository
import io.spine.server.entity.alter
import io.spine.server.route.EventRouting
import io.spine.server.route.Route
import io.spine.tools.mc.annotation.event.FileOptionMatched

/**
 * Gathers the API level options defined for an enum type.
 */
internal class EnumAnnotationsView : View<TypeName, EnumAnnotations, EnumAnnotations.Builder>() {

    @Subscribe
    fun on(e: FileOptionMatched) = alter {
        addOption(e.assumed)
    }

    @Subscribe
    fun on(@External e: EnumOptionDiscovered) = alter {
        addOption(e.option)
    }

    companion object {

        @Route
        fun route(e: FileOptionMatched) = e.toEnumTypeName()

        @Route
        fun route(e: EnumOptionDiscovered) = e.subject.name
    }

    class Repository: ViewRepository<TypeName, EnumAnnotationsView, EnumAnnotations>() {

        override fun setupEventRouting(routing: EventRouting<TypeName>) {
            super.setupEventRouting(routing)
            routing.route<FileOptionMatched> { e, _ ->
                e.toEnumTypeName()
            }.unicast<EnumOptionDiscovered> { e, _ ->
                e.subject.name
            }
        }
    }
}
