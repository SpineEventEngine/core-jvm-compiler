/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.tools.mc.java

import io.spine.tools.java.reference
import kotlin.text.RegexOption.DOT_MATCHES_ALL

/**
 * Tells if [javaCode] contains a class which implements the given [superInterface].
 */
fun implementsInterface(javaCode: String, superInterface: Class<*>): Boolean =
    implementsInterface(javaCode, superInterface.reference)

/**
 * Tells if [javaCode] contains a clas which implement the given [superInterface]
 * with generic parameters.
 */
fun implementsInterface(
    javaCode: String,
    superInterface: Class<*>,
    genericParameters: List<String>
): Boolean  {
    val parameters = genericParameters.joinToString(", ") { it }
    return implementsInterface(javaCode, "${superInterface.reference}<$parameters>")
}

/**
 * Tells if [javaCode] contains a class which implements the given [superInterface].
 */
private fun implementsInterface(javaCode: String, superInterface: String): Boolean {
    val regex = Regex("implements[^{}]*${superInterface}[^{}]*\\{", DOT_MATCHES_ALL)
    return regex.containsMatchIn(javaCode)
}
