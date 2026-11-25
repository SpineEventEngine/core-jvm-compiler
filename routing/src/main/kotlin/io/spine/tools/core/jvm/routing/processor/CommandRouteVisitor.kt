/*
 * Copyright 2026 CodeMatters, Lda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package io.spine.tools.core.jvm.routing.processor

import com.squareup.kotlinpoet.ksp.toClassName

/**
 * Creates a routing setup class for tuning
 * [EventRouting][io.spine.server.route.CommandRouting] of a repository.
 *
 * The generated setup class will have the name after the pattern
 * [&lt;EntityClass&gt;CommandRouting][classNameSuffix].
 * Please see [EntityClass.routingName] for entity classes nested in other classes.
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
