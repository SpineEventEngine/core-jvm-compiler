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

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Origin.JAVA
import com.google.devtools.ksp.symbol.Origin.JAVA_LIB

/**
 * Obtains the short name of the function.
 *
 * @returns just a name without braces.
 */
public val KSFunctionDeclaration.shortName: String
    get() = simpleName.getShortName()

/**
 * Selects either diagnostic message depending on
 * the [origin][KSFunctionDeclaration.origin] of the declaration.
 *
 * For origins [JAVA] and [JAVA_LIB] the value of the [java] parameter is returned.
 * Otherwise, the [kotlin] string is returned.
 */
public fun KSFunctionDeclaration.msg(kotlin: String, java: String): String =
    if (origin == JAVA || origin == JAVA_LIB) {
        java
    } else {
        kotlin
    }

/**
 * Obtains the text for referencing this function in a diagnostic message.
 */
public val KSFunctionDeclaration.diagRef: String
    get() {
        val shortRef = "`$shortName()`"
        return msg("function $shortRef", "method $shortRef")
    }
