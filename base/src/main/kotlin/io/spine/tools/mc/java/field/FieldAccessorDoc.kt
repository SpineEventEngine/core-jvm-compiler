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


package io.spine.tools.mc.java.field

import com.intellij.psi.javadoc.PsiDocComment
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.isList
import io.spine.protodata.ast.isMap
import io.spine.protodata.java.javaType
import io.spine.protodata.type.TypeSystem
import io.spine.tools.psi.java.Environment.elementFactory
import org.intellij.lang.annotations.Language

/**
 * The Javadoc of a method which returns a strongly typed proto field.
 *
 * @see FieldAccessor
 */
internal class FieldAccessorDoc(
    private val field: Field,
    private val typeSystem: TypeSystem
) {

    private val fieldName: String = field.name.value

    /**
     * An optional prefix before the name of a field in Javadoc.
     *
     * Is empty if the field is neither a map, nor repeated.
     */
    private val kind: String = when {
        field.isList -> "{@code repeated} "
        field.isMap -> "{@code map} "
        else -> ""
    }

    private val element: String = when {
        field.isList -> "element"
        field.isMap -> "value"
        else -> "field"
    }

    private val fieldType: String by lazy {
        field.javaType(typeSystem)
    }

    fun javadoc(): PsiDocComment {
        @Language("JAVA") @Suppress("EmptyClass")
        val result = elementFactory.createDocCommentFromText("""
            /**
             * Returns the $kind{@code $fieldName} field.
             *
             * <p>The $element Java type is {@code $fieldType}. 
             */
            """.trimIndent()
        )
        return result
    }
}
