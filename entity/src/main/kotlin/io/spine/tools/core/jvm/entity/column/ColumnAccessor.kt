/*
 * Copyright 2026 CodeMatters, Lda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package io.spine.tools.core.jvm.entity.column

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.javadoc.PsiDocComment
import io.spine.tools.compiler.ast.Field
import io.spine.tools.compiler.jvm.ClassName
import io.spine.tools.compiler.jvm.getterName
import io.spine.tools.compiler.jvm.javaCase
import io.spine.query.EntityColumn
import io.spine.tools.core.jvm.entity.TypeReferences
import io.spine.tools.java.reference
import io.spine.tools.psi.addFirst
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.addLast
import org.intellij.lang.annotations.Language

/**
 * The reference to the class that provides column information.
 */
internal val container = EntityColumn::class.java.reference

/**
 * Generates a method that returns a [strongly typed][EntityColumn] entity column.
 *
 * The name of the method matches the name of the [entity state][io.spine.base.EntityState]
 * converted to [javaCase].
 */
internal class ColumnAccessor(
    private val entityState: ClassName,
    private val field: Field,
    private val columnClass: PsiClass,
    private val typeReferences: TypeReferences
) {

    private val fieldName = field.name.value
    private val fieldType: String by lazy {
        typeReferences.of(field)
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
        columnType(entityState, fieldType)
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
 * the type of the field.
 *
 * @param entityState The name of the entity state class.
 * @param fieldType The reference to the type of the field of the column.
 *   Defaults to the wildcard `?`.
 */
internal fun columnType(entityState: ClassName, fieldType: String = "?"): String {
    // We can use a simple class name because the generated code is
    // nested inside the entity state class.
    val state = entityState.simpleName
    @Language("JAVA") @Suppress("EmptyClass")
    val result = "$container<$state, $fieldType>"
    return result
}
