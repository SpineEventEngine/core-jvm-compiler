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

package io.spine.tools.mc.java.routing

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper
import funRef
import io.spine.base.SignalMessage
import io.spine.core.SignalContext
import io.spine.server.route.Route
import io.spine.string.simply
import io.spine.tools.mc.java.routing.RouteSignature.Companion.qualify

/**
 * The base class for classes checking the contract of the functions with the [Route] annotation.
 *
 * The [match] function checks the signature of the function and creates an instance of
 * a class derived from [RouteFun], if the contract is satisfied.
 *
 * The [qualify] method of the companion object walks through the annotated functions
 * detected by the [RouteProcessor] matching them to corresponding signature kind and thus
 * producing proper [RouteFun] instances.
 */
internal sealed class RouteSignature<F : RouteFun>(
    protected val signalClass: Class<out SignalMessage>,
    protected val contextClass: Class<out SignalContext>,
    protected val context: Context
) {
    private val signalType by lazy { signalClass.toType(context.resolver) }
    private val contextType by lazy { contextClass.toType(context.resolver) }

    protected abstract fun matchDeclaringClass(
        fn: KSFunctionDeclaration,
        declaringClass: EntityClass
    ): Boolean

    @OverridingMethodsMustInvokeSuper
    protected open fun matchReturnType(
        fn: KSFunctionDeclaration,
        declaringClass: EntityClass
    ): KSType? {
        val idClass = declaringClass.idClass
        val returnType = fn.returnType!!.resolve()
        if (idClass.isAssignableFrom(returnType)) {
            return returnType
        }
        return null
    }

    /**
     * Creates a [RouteFun] of the type [F] for the given function and resolved types.
     */
    protected abstract fun create(
        fn: KSFunctionDeclaration,
        declaringClass: EntityClass,
        parameters: Pair<KSType, KSType?>,
        returnType: KSType
    ): F

    @Suppress("ReturnCount")
    fun match(fn: KSFunctionDeclaration, declaringClass: EntityClass): F? {
        val params = matchParameters(fn)
            ?: return null
        if (!matchDeclaringClass(fn, declaringClass)) {
            return null
        }
        val returnType = matchReturnType(fn, declaringClass)
            ?: return null
        return create(fn, declaringClass, params, returnType)
    }

    /**
     * Verifies that the given function accepts one or two parameters with
     * the types matching [signalClass] and [contextClass].
     *
     * The first parameter must be of [signalClass] or implement the interface specified
     * by this property.
     *
     * The second parameter, if any, must be of the [contextClass] type.
     */
    @OverridingMethodsMustInvokeSuper
    protected open fun matchParameters(fn: KSFunctionDeclaration): Pair<KSType, KSType?>? {
        checkParamSize(fn)

        val firstParamType = fn.parameters[0].type.resolve()
        if (!signalType.isAssignableFrom(firstParamType)) {
            // Even if the parameter does not match, it could be another kind of
            // routing function, so we simply return `false`.
            return null
        }
        var secondParamType: KSType? = null
        if (fn.parameters.size == 2) {
            secondParamType = fn.parameters[1].type.resolve()
            val match = contextType == secondParamType
            if (!match) {
                // Here, knowing that the first parameter type is correct, we can complain
                // about the type of the second parameter.
                val actualSecondParamName = secondParamType.declaration.simpleName.getShortName()
                context.logger.error(
                    "The second parameter of the ${fn.funRef} annotated with $routeRef" +
                            " must be `${contextClass.simpleName}`." +
                            " Encountered: `$actualSecondParamName`.",
                    fn
                )
                return null
            }
        }
        return Pair(firstParamType, secondParamType)
    }

    /**
     * A safety net to accept functions with the proper number of parameters.
     *
     * We formally check for this to be true in [KSFunctionDeclaration.acceptsOneOrTwoParameters].
     */
    private fun checkParamSize(fn: KSFunctionDeclaration) =
        require(fn.parameters.size == 1 || fn.parameters.size == 2)

    companion object {

        val routeRef by lazy { "`@${simply<Route>()}`" }
        val jvmStaticRef by lazy { "`@${simply<JvmStatic>()}`" }

        fun qualify(
            functions: Sequence<KSFunctionDeclaration>,
            context: Context
        ): List<RouteFun> {
            val qualifier = Qualifier(functions, context)
            return qualifier.run()
        }
    }
}
