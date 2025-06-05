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

import com.squareup.kotlinpoet.ksp.toClassName

/**
 * Creates a routing setup class for tuning
 * [EventRouting][io.spine.server.route.CommandRouting] of a repository.
 *
 * The generated setup class will have the name after the pattern
 * [&lt;EntityClass&gt;CommandRouting][classNameSuffix].
 *
 * @see RouteVisitor
 */
internal class CommandRouteVisitor(
    functions: List<CommandRouteFun>,
    environment: Environment
) : RouteVisitor<CommandRouteFun>(
    environment.commandRoutingSetup,
    functions,
    environment
) {
    override val classNameSuffix: String = "CommandRouting"

    override fun addRoute(fn: CommandRouteFun) {
        val params = if (fn.acceptsContext) "c, ctx" else "c"
        routingRunBlock.add(
            "%L<%T> { %L -> %T.%L(%L) }\n",
            ROUTE_FUN_NAME,
            fn.messageClass,
            params,
            entityClass.type.toClassName(),
            fn.decl.simpleName.asString(),
            params
        )
    }

    companion object {

        /**
         * Processes the given route functions using [CommandRouteVisitor].
         */
        fun process(qualified: List<RouteFun>, environment: Environment) {
            runVisitors<CommandRouteVisitor, CommandRouteFun>(qualified, environment) { functions ->
                CommandRouteVisitor(functions, environment)
            }
        }
    }
}
