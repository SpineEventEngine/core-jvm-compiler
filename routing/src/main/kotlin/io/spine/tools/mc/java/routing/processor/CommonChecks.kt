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

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier.JAVA_STATIC
import com.google.devtools.ksp.symbol.Origin.JAVA
import com.google.devtools.ksp.symbol.Origin.KOTLIN
import io.spine.server.entity.Entity
import io.spine.tools.mc.java.ksp.processor.diagRef
import io.spine.tools.mc.java.ksp.processor.msg
import io.spine.tools.mc.java.routing.processor.RouteSignature.Companion.routeRef

/**
 * Runs general usage checks for this function declaration.
 *
 * The function runs all the checks, assuming that compilation does not terminate after
 * an error is reported via [KSPLogger.error].
 *
 * @param environment The environment for resolving types and reporting errors or warnings.
 * @return The number of detected errors, or zero if no errors were found.
 */
internal fun KSFunctionDeclaration.commonChecks(environment: Environment): Boolean {
    val logger = environment.logger
    val declaredInAClass = declaredInAClass(logger)
    val isStatic = isStatic(logger)
    val acceptsOneOrTwoParameters = acceptsOneOrTwoParameters(logger)
    return (declaredInAClass
            && isStatic
            && acceptsOneOrTwoParameters)
}

private fun KSFunctionDeclaration.isStatic(logger: KSPLogger): Boolean {
    val isStatic = when (origin) {
        JAVA -> modifiers.contains(JAVA_STATIC)
        KOTLIN -> parentDeclaration is KSClassDeclaration &&
                (parentDeclaration as KSClassDeclaration).isCompanionObject
        else -> false
    } 
    if (!isStatic) {
        logger.error(msg(
            "The $diagRef annotated with $routeRef must be a member of a companion object.",
            "The $diagRef annotated with $routeRef must be `static`."
        ),
            this
        )
    }
    return isStatic
}

private fun KSFunctionDeclaration.declaredInAClass(logger: KSPLogger): Boolean {
    val inClass = parentDeclaration is KSClassDeclaration
    if (!inClass) {
        // This case is Kotlin-only because in Java a function would belong to a class.
        logger.error(
            "The $diagRef annotated with $routeRef must be" +
                    " a member of a companion object of an entity class.",
            this
        )
    }
    return inClass
}

private fun KSFunctionDeclaration.acceptsOneOrTwoParameters(logger: KSPLogger): Boolean {
    val wrongNumber = parameters.isEmpty() || parameters.size > 2
    if (wrongNumber) {
        logger.error(
            "The $diagRef annotated with $routeRef must accept one or two parameters. " +
                    "Encountered: ${parameters.size}.",
            this
        )
    }
    return (!wrongNumber)
}

/**
 * Obtains the entity class which declares this function.
 *
 * If the function is declared in a Kotlin companion object (which is the right way to declare
 * routing functions in Kotlin), the function obtains the class enclosing the companion object.
 *
 * The function checks if the declaring class implements the [Entity] interface.
 * If it does not, the error is logged using the logger of the [environment] pointing to
 * this function declaration as the source of the error, and `null` is returned.
 *
 * @return The entity class which declares this routing function, or `null` if the class
 *  does not implement the [Entity] interface.
 */
internal fun KSFunctionDeclaration.declaringClass(environment: Environment): EntityClass? {
    val parent = parentDeclaration!!.qualifiedName!!
    var declaringClass = environment.resolver.getClassDeclarationByName(parent)!!
    if (declaringClass.isCompanionObject) {
        // In Kotlin, routing functions are declared in a companion object.
        // So we "climb up" to the enclosing class.
        declaringClass = declaringClass.parentDeclaration!! as KSClassDeclaration
    }
    // Check that the class implements `Entity`.
    val projectedType = declaringClass.asStarProjectedType()
    if (!environment.entityInterface.isAssignableFrom(projectedType)) {
        environment.logger.error(
            "The declaring class of the $diagRef annotated with $routeRef" +
                    " must implement the `${Entity::class.java.canonicalName}` interface.",
            this
        )
        return null
    }
    return EntityClass(declaringClass, environment)
}

/**
 * Checks if given [functions] do not have the same type as the first parameter.
 *
 * If duplicates are found they are [reported][KSPLogger.error], and the function returns `true`.
 *
 * @return `true` if duplicating route functions found, `false` otherwise.
 */
internal fun <F : RouteFun> EntityClass.hasDuplicatedRoutes(
    functions: List<F>,
    environment: Environment
): Boolean {
    val logger = environment.logger
    val nl = System.lineSeparator()
    var found = false
    // Group functions by the first parameter.
    val grouped = functions.groupBy { fn -> fn.messageClass }
    grouped.forEach {
        var routes = it.value
        if (routes.size <= 1) {
            return@forEach
        }
        found = true
        // Sort duplicates by line numbers.
        routes = routes.sortedBy { fn -> fn.lineNumber }

        // The qualified message class to be mentioned once in the error message.
        // The functions will have the simple name.
        val messageType = routes[0].messageClass.canonicalName

        // List the duplicates.
        val duplicates = routes.joinToString(nl) { fn ->
            " * `${fn.asString(qualifiedParameters = false)}`"
        }
        logger.error(
            "The class `$this` declares more than one route function" +
                    " for the same message class `$messageType`:$nl" +
                    duplicates + nl +
                    "Please have only one function per routed message class.",
            // Give the last duplicate as the error pointer to be reported.
            // This would allow the user to scroll back for the duplicate(s).
            routes.last().decl
        )
    }
    return found
}
