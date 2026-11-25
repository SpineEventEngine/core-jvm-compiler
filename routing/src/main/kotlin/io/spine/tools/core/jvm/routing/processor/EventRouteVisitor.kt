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
 * [EventRouting][io.spine.server.route.EventRouting] of a repository.
 *
 * The generated setup class will have the name after the pattern
 * [&lt;EntityClass&gt;EventRouting][classNameSuffix]. Please see [EntityClass.routingName] for
 * the entity classes that are nested in other classes.
 *
 * @see MulticastRouteVisitor
 */
internal class EventRouteVisitor(
    functions: List<EventRouteFun>,
    environment: Environment
) : MulticastRouteVisitor<EventRouteFun>(
    environment.eventRoutingSetup,
    functions,
    environment
) {
    override val classNameSuffix: String = "EventRouting"
    override val messageParameterName: String = "e"

    companion object {

        /**
         * Processes the given route functions using [EventRouteVisitor].
         */
        internal fun process(qualified: List<RouteFun>, environment: Environment) {
            runVisitors<EventRouteVisitor, EventRouteFun>(qualified, environment) { functions ->
                EventRouteVisitor(functions, environment)
            }
        }
    }
}
