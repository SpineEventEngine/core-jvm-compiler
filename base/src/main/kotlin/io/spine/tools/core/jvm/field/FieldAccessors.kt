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

package io.spine.tools.core.jvm.field

import com.intellij.psi.PsiMethod
import com.intellij.psi.javadoc.PsiDocComment
import io.spine.tools.compiler.ast.Cardinality.CARDINALITY_SINGLE
import io.spine.tools.compiler.ast.Field
import io.spine.tools.compiler.ast.cardinality
import io.spine.tools.compiler.ast.isMessage
import io.spine.tools.compiler.ast.toMessageType
import io.spine.tools.compiler.ast.toType
import io.spine.tools.compiler.jvm.ClassName
import io.spine.tools.compiler.jvm.javaCase
import io.spine.tools.compiler.type.TypeSystem
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
     * The names of the classes that give access to nested fields.
     */
    private val names: FieldClassNames,

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
        return names.of(type.name)
    }
}

/**
 * Generates methods for fields belonging directly to the message type.
 */
internal class TopLevelFieldAccessor(
    field: Field,
    fieldSupertype: ClassName,
    names: FieldClassNames,
    typeSystem: TypeSystem
) : FieldAccessor(field, fieldSupertype, names, typeSystem) {

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
    names: FieldClassNames,
    typeSystem: TypeSystem
) : FieldAccessor(field, fieldSupertype, names, typeSystem) {

    override val modifiers: String = "public"

    override val methodBody: String by lazy {
        @Language("JAVA") @Suppress("EmptyClass", "NewClassNamingConvention")
        val result = """
            return new $returnType(getField().nested("$fieldName"));                
            """.trimIndent()
        result
    }
}
