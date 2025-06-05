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

import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toClassName
import io.spine.tools.mc.java.ksp.processor.shortName

/**
 * Provides information about a route function detected in the [declaringClass]
 *
 * @property decl The declaration of the function
 * @property declaringClass The class that declares the function.
 * @param parameters The parameter(s) of the function.
 *  If the [Pair.second] property is non-null the function accepts a context parameter.
 * @param returnType The type returned by the function.
 */
internal sealed class RouteFun(
    val decl: KSFunctionDeclaration,
    val declaringClass: EntityClass,
    parameters: Pair<KSType, KSType?>,
    returnType: KSType
) {
    /**
     * The line number of the function declaration.
     */
    val lineNumber: Int by lazy {
        (decl.location as? FileLocation)?.lineNumber ?: 0
    }

    /**
     * The type of the first parameter of the route function.
     */
    val messageParameter: KSType = parameters.first

    /**
     * The class name of the first parameter.
     */
    val messageClass: ClassName = messageParameter.toClassName()

    /**
     * Tells if the function accepts a context parameter.
     */
    val acceptsContext: Boolean = parameters.second != null

    /**
     * The class of the message context, if the function accepts the second parameter.
     */
    private val contextClass: ClassName? by lazy {
        parameters.second?.toClassName()
    }

    /**
     * Tells if the function returns one identifier rather than a set of identifiers.
     */
    val isUnicast: Boolean = returnType.declaration.typeParameters.isEmpty()

    /**
     * Obtains the name of the function with the types of its parameters.
     *
     * @param qualifiedParameters If `true` the parameter types will be fully qualified.
     *  Otherwise, simple names will be used for the parameter types.
     */
    fun asString(qualifiedParameters: Boolean): String {
        fun ClassName.name(): String = if (qualifiedParameters) canonicalName else simpleName
        return buildString {
            append(decl.shortName)
            append("(")
            append(messageClass.name())
            if (acceptsContext) {
                append(", ")
                append(contextClass!!.name())
            }
            append(")")
        }
    }

    /**
     * Gives the name of the function with fully qualified names for parameters.
     */
    override fun toString(): String = asString(qualifiedParameters = true)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteFun

        if (decl != other.decl) return false
        if (declaringClass != other.declaringClass) return false

        return true
    }

    override fun hashCode(): Int {
        var result = decl.hashCode()
        result = 31 * result + declaringClass.hashCode()
        return result
    }
}

/**
 * The declaration of a route function for commands.
 */
internal class CommandRouteFun(
    fn: KSFunctionDeclaration,
    declaringClass: EntityClass,
    parameters: Pair<KSType, KSType?>,
    returnType: KSType
) : RouteFun(fn, declaringClass, parameters, returnType)

/**
 * The declaration of a route function for events.
 */
internal class EventRouteFun(
    fn: KSFunctionDeclaration,
    declaringClass: EntityClass,
    parameters: Pair<KSType, KSType?>,
    returnType: KSType
) : RouteFun(fn, declaringClass, parameters, returnType)

/**
 * The declaration of a route function for entity states.
 */
internal class StateUpdateRouteFun(
    fn: KSFunctionDeclaration,
    declaringClass: EntityClass,
    parameters: Pair<KSType, KSType?>,
    returnType: KSType
) : RouteFun(fn, declaringClass, parameters, returnType)
