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

package io.spine.tools.mc.java.routing.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSType
import io.spine.server.aggregate.Aggregate
import io.spine.server.entity.Entity
import io.spine.server.procman.ProcessManager
import io.spine.server.projection.Projection
import io.spine.server.route.CommandRouting
import io.spine.server.route.setup.CommandRoutingSetup
import io.spine.server.route.EventRouting
import io.spine.server.route.setup.EventRoutingSetup
import io.spine.server.route.MessageRouting
import io.spine.server.route.setup.StateRoutingSetup
import io.spine.server.route.StateUpdateRouting
import io.spine.tools.mc.java.ksp.processor.toType
import kotlin.reflect.KClass

/**
 * Provides instances required for resolving types or reporting errors or warnings.
 */
internal class Environment(
    val resolver: Resolver,
    val logger: KSPLogger,
    val codeGenerator: CodeGenerator
) {
    val entityInterface by lazy { Entity::class.toType(resolver) }
    val aggregateClass by lazy { Aggregate::class.toType(resolver) }
    val projectionClass by lazy { Projection::class.toType(resolver) }
    val processManagerClass by lazy { ProcessManager::class.toType(resolver) }
    val setClass by lazy { Set::class.toType(resolver) }

    val commandRoutingSetup = SetupType(CommandRoutingSetup::class, CommandRouting::class)
    val eventRoutingSetup = SetupType(EventRoutingSetup::class, EventRouting::class)
    val stateRoutingSetup = SetupType(StateRoutingSetup::class, StateUpdateRouting::class)

    inner class SetupType(
        val cls: KClass<out Any>,
        val routingClass: KClass<out MessageRouting<*, *, *, *, *>>
    ) {
        val type: KSType by lazy { cls.toType(resolver) }
    }
}

