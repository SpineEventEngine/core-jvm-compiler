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

package io.spine.tools.mc.java.annotation

import io.spine.protodata.render.SourceFile
import io.spine.tools.code.Java
import io.spine.tools.mc.annotation.ApiOption
import java.io.File

/**
 * Abstract base for annotators that process the Java code basing on
 * the patterns passing via [Settings].
 */
internal abstract class PatternAnnotator : Annotator() {

    private val patterns: List<Regex> by lazy {
        loadPatterns().map {
            it.toRegex()
        }
    }

    /**
     * The type of the annotation to be used in the generated code.
     */
    protected val annotationClass: Class<Annotation> by lazy {
        annotationClass(ApiOption.INTERNAL)
    }

    /**
     * Loads the list of patterns to be used by this renderer.
     */
    abstract fun loadPatterns(): List<String>

    /**
     * Tells if the given code element matches one of the patterns given in settings.
     */
    protected fun matches(codeElement: String): Boolean =
        patterns.any { it.matches(codeElement) }
}

internal fun SourceFile<Java>.qualifiedTopClassName(): String
    = relativePath.toString().replace(File.separator, ".").replace(".java", "")
