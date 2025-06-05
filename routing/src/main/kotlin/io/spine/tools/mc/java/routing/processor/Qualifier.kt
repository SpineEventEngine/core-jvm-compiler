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

import com.google.common.annotations.VisibleForTesting
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

/**
 * The helper class which transforms the incoming sequence with [functions] into
 * a list containing [CommandRouteFun], [EventRouteFun], or [StateUpdateRouteFun].
 *
 * If a function is not recognized to be one of these types,
 * the compilation terminates with an error.
 */
internal class Qualifier(
    private val functions: Sequence<KSFunctionDeclaration>,
    private val environment: Environment
) {
    private val commandRoutes = CommandRouteSignature(environment)
    private val eventRoutes = EventRouteSignature(environment)
    private val stateRoutes = StateUpdateRouteSignature(environment)

    /**
     * Transforms the incoming sequence of [KSFunctionDeclaration] instances
     * into the list of [RouteFun] by analyzing their signatures.
     *
     * Each function goes through [common checks][KSFunctionDeclaration.commonChecks].
     * Failed checks are reported as errors via [Environment.logger].
     *
     * If at least one error is detected, the function terminates with [IllegalStateException]
     * after all the functions are checked.
     */
    fun run(): List<RouteFun> {
        val result = mutableListOf<RouteFun>()
        functions.forEach { fn ->
            if (!fn.commonChecks(environment)) {
                return@forEach
            }
            val declaringClass = fn.declaringClass(environment)
                ?: return@forEach

            val qualified = qualify(fn, declaringClass)
            if (qualified != null) {
                result.add(qualified)
            } else {
                environment.logger.error(
                    "$errorMsgPrefix`${fn.qualifiedName?.asString()}`.", fn
                )
            }
        }
        return result
    }

    @Suppress("ReturnCount")
    private fun qualify(fn: KSFunctionDeclaration, declaringClass: EntityClass): RouteFun? {
        commandRoutes.match(fn, declaringClass)?.let {
            return it
        } ?: eventRoutes.match(fn, declaringClass)?.let {
            return it
        } ?: stateRoutes.match(fn, declaringClass)?.let {
            return it
        }
        return null
    }

    companion object {

        /**
         * The prefix used by [Qualifier] for reporting a function
         * which was not recognized as a valid routing function.
         */
        @VisibleForTesting
        const val errorMsgPrefix = "Unqualified function encountered: "
    }
}
