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
import com.google.devtools.ksp.symbol.KSType
import kotlin.reflect.KClass

/**
 * Converts this class into [KSType] using the given resolver.
 */
public fun KClass<*>.toType(resolver: Resolver): KSType {
    val name = resolver.getKSNameFromString(qualifiedName!!)
    val classDecl = resolver.getClassDeclarationByName(name)
    // This is a reminder to add corresponding JAR to `KotlinCompilation` in tests.
    check(classDecl != null) {
        "Unable to find the declaration of `$qualifiedName!!`." +
                " Make sure the class is in the compilation classpath."
    }
    val type = classDecl.asStarProjectedType()
    return type
}

/**
 * Converts this class into [KSType] using the given resolver.
 */
public fun Class<*>.toType(resolver: Resolver): KSType = kotlin.toType(resolver)

/**
 * Tells if this type has the same qualified name as the given one.
 */
public fun KSType.isSame(other: KSType): Boolean =
    declaration.qualifiedName?.asString() == other.declaration.qualifiedName?.asString()
