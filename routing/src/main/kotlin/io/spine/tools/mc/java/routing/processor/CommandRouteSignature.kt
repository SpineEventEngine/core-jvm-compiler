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
import io.spine.base.CommandMessage
import io.spine.core.CommandContext
import io.spine.tools.mc.java.ksp.processor.qualifiedRef
import io.spine.tools.mc.java.ksp.processor.ref

internal class CommandRouteSignature(
    environment: Environment
) : RouteSignature<CommandRouteFun>(
    CommandMessage::class.java,
    CommandContext::class.java,
    environment
) {
    override fun matchDeclaringClass(
        fn: KSFunctionDeclaration,
        declaringClass: EntityClass
    ): Boolean = environment.run {
        val isAggregate = aggregateClass.isAssignableFrom(declaringClass.type)
        val isProcessManager = processManagerClass.isAssignableFrom(declaringClass.type)
        val match = isAggregate || isProcessManager
        if (!match) {
            val parent = declaringClass.superClass()
            logger.error(
                "A command routing function can be declared in a class derived" +
                        " from ${processManagerClass.ref} or ${aggregateClass.ref}." +
                        " Encountered: ${parent.qualifiedRef}.",
            fn)
        }
        return match
    }

    override fun create(
        fn: KSFunctionDeclaration,
        declaringClass: EntityClass,
        parameters: Pair<KSType, KSType?>,
        returnType: KSType
    ): CommandRouteFun = CommandRouteFun(fn, declaringClass, parameters, returnType)
}
