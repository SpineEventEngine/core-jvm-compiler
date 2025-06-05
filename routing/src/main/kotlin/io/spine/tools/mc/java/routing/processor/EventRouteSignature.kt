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

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.spine.base.EventMessage
import io.spine.core.EventContext
import io.spine.tools.mc.java.ksp.processor.qualifiedRef
import io.spine.tools.mc.java.ksp.processor.ref

internal class EventRouteSignature(
    environment: Environment
) : RouteSignature<EventRouteFun>(
    EventMessage::class.java,
    EventContext::class.java,
    environment
) {
    override fun matchDeclaringClass(
        fn: KSFunctionDeclaration,
        declaringClass: EntityClass
    ): Boolean = environment.run {
        val isAggregate = aggregateClass.isAssignableFrom(declaringClass.type)
        val isProjection = projectionClass.isAssignableFrom(declaringClass.type)
        val isProcessManager = processManagerClass.isAssignableFrom(declaringClass.type)
        val match = isAggregate || isProjection || isProcessManager
        if (!match) {
            val parent = declaringClass.superClass()
            logger.error(
                "An event routing function can be declared in a class derived" +
                        " from ${processManagerClass.ref} or ${aggregateClass.ref} or" +
                        " ${projectionClass.ref}." +
                        " Encountered: ${parent.qualifiedRef}.",
            fn)
        }
        return match
    }

    @Suppress("ReturnCount") // Prefer a sooner exit to reduce nesting.
    override fun matchReturnType(
        fn: KSFunctionDeclaration,
        declaringClass: EntityClass
    ): KSType? = environment.run {
        val unicast = super.matchReturnType(fn, declaringClass)
        if (unicast != null) {
            return unicast
        }
        // Return type is not the entity ID.
        val returnType = fn.returnType?.resolve()!!
        if (!setClass.isAssignableFrom(returnType)) {
            logger.error(
                "A routing function for events routed to" +
                        " `$declaringClass` must return either an identifier" +
                        " of the type ${declaringClass.idClass.qualifiedRef}" +
                        " or a ${setClass.ref} of entity identifiers." +
                        " Encountered: ${returnType.qualifiedRef}.",
                fn
            )
            return null
        }
        // The returned type is a `Set`. Let's check the generic argument.
        val firstArg = returnType.arguments.firstOrNull()
        if (firstArg == null) {
            logger.error(
                "A multicast routing function for events must return" +
                        " a `Set` whose generic argument is an entity identifier." +
                        " Encountered: no argument.",
                fn
            )
            return null
        }
        val argumentClass = firstArg.type!!.resolve()
        if (!declaringClass.idClass.isAssignableFrom(argumentClass)) {
            logger.error(
                "A multicast routing function for events must return" +
                        " a `Set` whose generic argument is an entity identifier." +
                        " Expected: ${declaringClass.idClass.ref}." +
                        " Encountered: ${argumentClass.qualifiedRef}.",
                fn
            )
            return null
        }
        return returnType
    }

    override fun create(
        fn: KSFunctionDeclaration,
        declaringClass: EntityClass,
        parameters: Pair<KSType, KSType?>,
        returnType: KSType
    ): EventRouteFun = EventRouteFun(fn, declaringClass, parameters, returnType)
}
