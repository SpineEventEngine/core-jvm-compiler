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

package io.spine.tools.mc.java.entity.column

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.javadoc.PsiDocComment
import io.spine.protodata.ast.Field
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.getterName
import io.spine.protodata.java.javaCase
import io.spine.protodata.java.typeReference
import io.spine.protodata.type.TypeSystem
import io.spine.query.EntityColumn
import io.spine.tools.java.reference
import io.spine.tools.psi.addFirst
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.addLast
import org.intellij.lang.annotations.Language

/**
 * The reference to the class which provides column information.
 */
internal val container = EntityColumn::class.java.reference

/**
 * Generates a method which returns a [strongly typed][EntityColumn] entity column.
 *
 * The name of the method matches the name of the [entity state][io.spine.base.EntityState]
 * converted to [javaCase].
 */
internal class ColumnAccessor(
    private val entityState: ClassName,
    private val field: Field,
    private val columnClass: PsiClass,
    private val typeSystem: TypeSystem
) {

    private val fieldName = field.name.value
    private val fieldType by lazy {
        field.typeReference(entityState, typeSystem)
    }

    /**
     * The reference to the entity state class.
     *
     * Since the column class is nested in the entity state class,
     * it is safe to use a simple class name.
     */
    private val stateRef = entityState.simpleName

    /**
     * The return type of the generated method.
     */
    private val columnType: String by lazy {
        columnType(entityState, typeSystem, field)
    }

    /**
     * The method reference used in the generated code as an argument passed to
     * the constructor of [container].
     */
    private val getterRef: String by lazy {
        "$stateRef::${field.getterName}"
    }

    private val methodName: String
        get() = columnMethodName(this.field)

    private val javadoc: PsiDocComment by lazy {
        @Language("JAVA") @Suppress("EmptyClass")
        val doc = elementFactory.createDocCommentFromText("""
            /**
             * Returns the {@code "$fieldName"} column.
             *
             * <p>The Java type of the column is {@code $fieldType}.
             */           
            """.trimIndent()
        )
        doc
    }

    private val method: PsiMethod by lazy {
        @Language("JAVA") @Suppress("EmptyClass", "NewClassNamingConvention")
        val newMethod = elementFactory.createMethodFromText("""
            public static $columnType $methodName() {
              return new $container<>("$fieldName", $fieldType.class, $getterRef);    
            }                                
            """.trimIndent(), columnClass
        )
        newMethod.addFirst(javadoc)
        newMethod
    }

    /**
     * Adds the method to [columnClass].
     */
    fun render() {
        columnClass.addLast(method)
    }
}

/**
 * Obtains a name for accessing the column for the given field.
 */
internal fun columnMethodName(field: Field): String =
    field.name.javaCase()

/**
 * Obtains a string with the name of an entity column parameterized by
 * the type of the field, if specified.
 *
 * @param entityState The name of the entity state class.
 * @param typeSystem The instance of the [TypeSystem] to resolve the type of the given [field].
 *   Can be `null`, if [field] is `null`.
 * @param field The field of the column for composing the type.
 *   It is `null`, if the method is called for obtaining wildcard generic type name.
 */
internal fun columnType(
    entityState: ClassName,
    typeSystem: TypeSystem? = null,
    field: Field? = null
): String {
    require(!(typeSystem == null && field != null)) {
        "Unable to obtain a field type without type system."
    }
    // We can use a simple class name because the generated code is
    // nested inside the entity state class.
    val state = entityState.simpleName

    val fieldType = field?.typeReference(entityState, typeSystem!!) ?: "?"
    @Language("JAVA") @Suppress("EmptyClass")
    val result = "$container<$state, $fieldType>"
    return result
}
