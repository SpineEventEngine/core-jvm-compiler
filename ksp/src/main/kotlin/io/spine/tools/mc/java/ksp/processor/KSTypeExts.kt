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

package io.spine.tools.mc.java.ksp.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind.INTERFACE
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.Variance

/**
 * Tells if this type represents an interface.
 */
public val KSType.isInterface: Boolean
    get() = (declaration is KSClassDeclaration)
            && (declaration as KSClassDeclaration).classKind == INTERFACE

/**
 * Obtains a simple name of the type surrounded with back ticks.
 */
public val KSType.ref: String
    get() = "`${declaration.simpleName.asString()}`"

/**
 * Obtains a qualified name of the type surrounded with back ticks.
 */
public val KSType.qualifiedRef: String
    get() = "`${declaration.qualifiedName?.asString()}`"

/**
 * Transforms this instance of [KSType] to an instance of [KSTypeArgument]
 * using the given [resolver].
 *
 * @param resolver The resolver to create the type argument instance.
 * @param variance The variance to use for the type argument.
 *  The default value is [Variance.INVARIANT].
 */
public fun KSType.toTypeArgument(
    resolver: Resolver,
    variance: Variance = Variance.INVARIANT
): KSTypeArgument {
    val typeRef = resolver.createKSTypeReferenceFromKSType(this)
    return resolver.getTypeArgument(typeRef, variance)
}
