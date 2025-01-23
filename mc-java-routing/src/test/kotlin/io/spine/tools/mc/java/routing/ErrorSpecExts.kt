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

import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.SourceFile.Companion.java
import java.io.File
import org.intellij.lang.annotations.Language

/**
 * Obtains the path to the classpath element which contains the receiver class.
 */
internal fun Class<*>.classpathFile(): File = File(protectionDomain.codeSource.location.path)

/**
 * The package directory is `io/spine/given/devices/` which matches the options of
 * proto types defined under `test/proto/given/devices/`.
 */
private val packageDir = "io/spine/given/devices"

/**
 * Obtains a file name with the package directories for a Java class with the given simple name.
 */
private fun javaFn(simpleName: String): String = "$packageDir/${simpleName}.java"

/**
 * Obtains a file name with the package directories for a Kotlin file with the given name.
 */
internal fun kotlinFile(simpleName: String): String = "$packageDir/${simpleName}.kt"

/**
 * Creates an instance of [SourceFile] with the Java file containing the class
 * with the specified name.
 */
internal fun javaFile(
    simpleClassName: String,
    @Language("java") contents: String
): SourceFile {
    return java(name = javaFn(simpleClassName), contents = contents, trimIndent = true)
}
