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

/**
 * Creates a routing setup class for tuning
 * [StateUpdateRouting][io.spine.server.route.StateUpdateRouting] of a repository.
 *
 * The generated setup class will have the name after the pattern
 * [&lt;EntityClass&gt;StateUpdateRouting][classNameSuffix].
 * Please see [EntityClass.routingName] for entity classes nested in other classes.
 *
 * @see MulticastRouteVisitor
 */
internal class StateUpdateRouteVisitor(
    functions: List<StateUpdateRouteFun>,
    environment: Environment
) : MulticastRouteVisitor<StateUpdateRouteFun>(
    environment.stateRoutingSetup,
    functions,
    environment
) {
    override val classNameSuffix: String = "StateUpdateRouting"
    override val messageParameterName: String = "s"

    companion object {

        /**
         * Processes the given route functions using [StateUpdateRouteVisitor].
         */
        fun process(qualified: List<RouteFun>, environment: Environment) {
            runVisitors<StateUpdateRouteVisitor, StateUpdateRouteFun>(
                qualified,
                environment
            ) { functions ->
                StateUpdateRouteVisitor(functions, environment)
            }
        }
    }
}
