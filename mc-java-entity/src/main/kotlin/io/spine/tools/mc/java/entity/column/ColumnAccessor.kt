/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.tools.mc.java.entity.column

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.javadoc.PsiDocComment
import io.spine.protodata.Field
import io.spine.protodata.codegen.java.ClassName
import io.spine.protodata.codegen.java.getterName
import io.spine.protodata.codegen.java.javaCase
import io.spine.protodata.codegen.java.javaType
import io.spine.protodata.codegen.java.reference
import io.spine.protodata.type.TypeSystem
import io.spine.query.EntityColumn
import io.spine.tools.psi.java.PsiWrite.elementFactory
import org.intellij.lang.annotations.Language

/**
 * Generates a method which returns a [strongly typed][EntityColumn] entity column.
 *
 * The name of the method matches the name of the [entity state][io.spine.base.EntityState]
 * converted to [javaCase].
 */
internal class ColumnAccessor(
    private val typeSystem: TypeSystem,
    private val entityState: ClassName,
    private val field: Field,
    private val wrappingClass: PsiClass
) {

    private val fieldName = field.name.value
    private val fieldType by lazy {
        field.javaType(typeSystem)
    }

    fun method(): PsiMethod {
        val columnType = columnType(entityState, typeSystem, field)
        @Suppress("EmptyClass")
        val getterRef = "$stateRef::${field.getterName}"
        @Language("JAVA")
        val method = elementFactory.createMethodFromText("""
            public static $columnType $methodName() {
              return new $container<>("$fieldName", $fieldType.class, $getterRef);    
            }                                
            """.trimIndent(), wrappingClass
        )
        method.addBefore(javaDoc(), method.firstChild)
        return method
    }

    private val stateRef = entityState.canonical

    private val methodName: String
        get() = columnMethodName(this.field)

    private fun javaDoc(): PsiDocComment {
        val methodDoc = elementFactory.createDocCommentFromText("""
        /**
         * Returns the {@code "$fieldName"} column.
         *
         * <p>The Java type of the column is {@code $fieldType}.
         */           
        """.trimIndent()
        )
        return methodDoc
    }
}

/**
 * Obtains a name for accessing the column for the given field.
 */
internal fun columnMethodName(field: Field): String =
    field.name.javaCase()

/**
 * Obtains a string with the of an entity column parameterized by
 * the type of the field, if specified.
 *
 * @param entityState
 *         the name of the entity state class.
 * @param typeSystem
 *         the instance of the [TypeSystem] to resolve the type of the given [field].
 *         Can be `null`, if [field] is `null`.
 * @param field
 *         the field of the column for composing the type.
 *         It is `null`, if the method is called for obtaining wildcard generic type name.
 */
internal fun columnType(
    entityState: ClassName,
    typeSystem: TypeSystem? = null,
    field: Field? = null
): String {
    require(!(typeSystem == null && field != null)) {
        "Unable to obtain a field type without type system."
    }
    val fieldType = field?.javaType(typeSystem!!) ?: "?"
    val state = entityState.canonical
    return "$container<$state, $fieldType>"
}

private val container = EntityColumn::class.reference
