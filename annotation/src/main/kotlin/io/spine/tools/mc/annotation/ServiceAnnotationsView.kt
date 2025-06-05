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
import io.spine.protodata.ast.ServiceName
import io.spine.protodata.ast.event.ServiceOptionDiscovered
import io.spine.protodata.plugin.View
import io.spine.server.entity.alter
import io.spine.server.route.Route
import io.spine.tools.mc.annotation.event.FileOptionMatched

/**
 * Gathers the options defined for a service.
 *
 * Subscribes to [ServiceOptionDiscovered] for obtaining directly set options.
 *
 * Subscribes to [FileOptionMatched] events for getting matches between file level options,
 * and type options that are assumed for all the types in the file.
 */
internal class ServiceAnnotationsView :
    View<ServiceName, ServiceAnnotations, ServiceAnnotations.Builder>() {

    @Subscribe
    fun on(@External e: ServiceOptionDiscovered) = alter {
        file = e.file
        // If the option was defined at the file level, overwrite it.
        optionBuilderList.find { it.name == e.option.name }?.let {
            it.value = e.option.value
            return@alter
        }
        addOption(e.option)
    }

    @Subscribe
    fun on(e: FileOptionMatched) = alter {
        file = e.file
        // If the option is already present at the service level, do not overwrite it.
        optionList.find { it.name == e.assumed.name }?.let {
            return@alter
        }
        addOption(e.assumed)
    }

    companion object {

        @Route
        fun route(e: FileOptionMatched) = e.toServiceName()

        @Route
        fun route(e: ServiceOptionDiscovered) = e.subject.name
    }
}
