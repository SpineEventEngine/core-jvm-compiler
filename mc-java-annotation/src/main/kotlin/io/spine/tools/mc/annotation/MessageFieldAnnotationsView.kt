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
import io.spine.protodata.ast.event.FieldOptionDiscovered
import io.spine.protodata.plugin.View
import io.spine.protodata.plugin.ViewRepository
import io.spine.server.entity.alter
import io.spine.server.route.EventRouting

/**
 * Gathers API level options defined for fields of a message.
 *
 * Subscribes to [FieldOptionDiscovered] for obtaining directly set options.
 */
internal class MessageFieldAnnotationsView:
    View<TypeName, MessageFieldAnnotations, MessageFieldAnnotations.Builder>()  {

    @Subscribe
    fun on(@External e: FieldOptionDiscovered) = alter {
        file = e.file
        val fieldName = e.subject.name
        val fieldOptions = fieldOptionsBuilderList.find { it.field == fieldName }
        fieldOptions?.let {
            it.addOption(e.option)
            return@alter
        }
        addFieldOptions(
            fieldOptions {
                field = fieldName
                option.add(e.option)
            }
        )
    }

    /**
     * The repository for [MessageAnnotationsView] which tunes the routing of events.
     */
    class Repository :
        ViewRepository<TypeName, MessageFieldAnnotationsView, MessageFieldAnnotations>() {

        override fun setupEventRouting(routing: EventRouting<TypeName>) {
            super.setupEventRouting(routing)
            routing.route<FieldOptionDiscovered> { e, _ ->
                if (ApiOption.findMatching(e.option) != null) {
                    setOf(e.subject.declaringType)
                } else {
                    emptySet()
                }
            }
        }
    }
}
