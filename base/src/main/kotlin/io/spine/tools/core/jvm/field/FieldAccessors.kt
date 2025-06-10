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

import com.intellij.psi.PsiMethod
import com.intellij.psi.javadoc.PsiDocComment
import io.spine.protodata.ast.Cardinality.CARDINALITY_SINGLE
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.cardinality
import io.spine.protodata.ast.isMessage
import io.spine.protodata.ast.toMessageType
import io.spine.protodata.ast.toType
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.javaCase
import io.spine.protodata.type.TypeSystem
import io.spine.tools.java.reference
import io.spine.tools.psi.addFirst
import io.spine.tools.psi.java.Environment.elementFactory
import org.intellij.lang.annotations.Language

/**
 * Abstract base for generating a method accessing a message field via a generated `Field` class.
 *
 * @see AddFieldClass
 */
internal abstract class FieldAccessor(
    /**
     * The field of the message type for which we generate the method.
     */
    private val field: Field,

    /**
     * The type of the returned field object for a simple field type, and a superclass
     * for accessing nested fields for a message field type.
     *
     * @see io.spine.base.EventMessageField
     * @see io.spine.query.EntityStateField
     */
    private val fieldSupertype: ClassName,

    /**
     * The type system to obtain Java class names by message types.
     */
    private val typeSystem: TypeSystem
) {

    /**
     * Access modifiers for a method.
     */
    protected abstract val modifiers: String

    /**
     * The code of the method body.
     */
    protected abstract val methodBody: String

    /**
     * Creates a [PsiMethod] with the code for accessing the field.
     */
    internal fun method(): PsiMethod {
        @Language("JAVA") @Suppress("EmptyClass", "NewClassNamingConvention")
        val method = elementFactory.createMethodFromText("""
            $modifiers $returnType $methodName() {
                $methodBody
            }  
            """.trimIndent(), null
        )
        method.addFirst(javadoc)
        return method
    }

    /**
     * The name of the field.
     */
    protected val fieldName: String = field.name.value

    /**
     * The type returned by the method.
     */
    protected val returnType: String by lazy {
        if (shouldExposeNestedFields) {
            nestedFieldsContainerType()
        } else {
            simpleFieldType
        }
    }

    private val shouldExposeNestedFields: Boolean =
        field.isMessage && field.type.cardinality == CARDINALITY_SINGLE

    private val simpleFieldType: String by lazy {
        fieldSupertype.canonical
    }

    private val methodName: String by lazy {
        field.name.javaCase()
    }

    private val javadoc: PsiDocComment by lazy {
        FieldAccessorDoc(field, typeSystem).javadoc()
    }

    private fun nestedFieldsContainerType(): String {
        check(field.isMessage)
        val type = field.toType().toMessageType(typeSystem)
        return MessageTypedField.classNameFor(type)
    }
}

/**
 * Generates methods for fields belonging directly to the message type.
 */
internal class TopLevelFieldAccessor(
    field: Field,
    fieldSupertype: ClassName,
    typeSystem: TypeSystem
) : FieldAccessor(field, fieldSupertype, typeSystem) {

    override val modifiers: String = "public static"

    override val methodBody: String by lazy {
        val fieldClass = io.spine.base.Field::class.java.reference
        @Language("JAVA") @Suppress("EmptyClass", "NewClassNamingConvention")
        val result = """
            return new $returnType($fieldClass.named("$fieldName"));            
            """.trimIndent()
        result
    }
}

/**
 * Generates methods for fields belonging to a message type that is the type of
 * the field directly belonging to the message type.
 */
internal class NestedFieldAccessor(
    field: Field,
    fieldSupertype: ClassName,
    typeSystem: TypeSystem
) : FieldAccessor(field, fieldSupertype, typeSystem) {

    override val modifiers: String = "public"

    override val methodBody: String by lazy {
        @Language("JAVA") @Suppress("EmptyClass", "NewClassNamingConvention")
        val result = """
            return new $returnType(getField().nested("$fieldName"));                
            """.trimIndent()
        result
    }
}
