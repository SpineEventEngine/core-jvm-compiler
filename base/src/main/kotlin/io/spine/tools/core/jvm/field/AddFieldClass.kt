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

import com.google.protobuf.StringValue
import com.intellij.psi.PsiClass
import io.spine.tools.compiler.ast.Cardinality.CARDINALITY_SINGLE
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.compiler.ast.MessageTypeDependencies
import io.spine.tools.compiler.context.CodegenContext
import io.spine.tools.compiler.jvm.ClassName
import io.spine.tools.compiler.render.SourceFile
import io.spine.tools.code.Java
import io.spine.tools.core.jvm.CreateReservedClass
import io.spine.tools.core.jvm.field.AddFieldClass.Companion.NAME
import io.spine.tools.psi.java.addLast
import org.intellij.lang.annotations.Language

/**
 * Creates a nested class called [`Field`][NAME] under a Java class generated for
 * the given message [type].
 *
 * The name is reserved: the message, a type enclosing it, and a type nested directly in it,
 * cannot have it.
 *
 * The class contains classes for the types of message fields. They are named after the types, and
 * have different names even if the types have the same name.
 *
 * @param type The message type for the Java code of which to generate the nested class.
 * @param file The file with Java code generated for the [type].
 * @property fieldSupertype The class name for the supertype of generated nested field classes,
 *   e.g., [io.spine.base.EventMessageField] or [io.spine.query.EntityStateField].
 * @property context The code generation context under which this code generation action runs.
 */
public open class AddFieldClass(
    type: MessageType,
    file: SourceFile<Java>,
    fieldSuperClassName: StringValue,
    context: CodegenContext
) : CreateReservedClass(type, file, NAME, context) {

    private val fieldSupertype: ClassName = fieldSuperClassName.value.toClassName()

    private val fieldTypes: Set<MessageType> by lazy {
        MessageTypeDependencies(
            messageType = type,
            cardinality = CARDINALITY_SINGLE,
            typeSystem = typeSystem
        ).asSet()
    }

    private val names: FieldClassNames by lazy {
        FieldClassNames(
            messageType = type,
            fieldClass = messageClass.nested(simpleName),
            fieldTypes = fieldTypes.map { it.name }.toSet()
        )
    }

    public companion object {

        /**
         * The name of the generated Java class.
         */
        public const val NAME: String = "Field"
    }

    @Language("JAVA") @Suppress("EmptyClass")
    override fun classJavadoc(): String = """
        /**
         * The listing of $messageJavadocRef fields to be used for creating a subscription filter.
         *
         * <p>Please use static methods of this class to access top-level fields of
         * the entity state type.
         * 
         * <p>Nested fields can be accessed using the values returned by the top-level
         * field accessors, through method chaining.
         */ 
        """.trimIndent()

    override fun tuneClass() {
        cls.addTopLevelFieldMethods()
        cls.addFieldClasses()
    }

    private fun PsiClass.addTopLevelFieldMethods() {
        type.fieldList.forEach {
            val accessor = TopLevelFieldAccessor(
                field = it,
                fieldSupertype = fieldSupertype,
                names = names,
                typeSystem = typeSystem
            )
            addLast(accessor.method())
        }
    }

    private fun PsiClass.addFieldClasses() {
        fieldTypes.forEach {
            val fld = MessageTypedField(
                fieldType = it,
                fieldSupertype = fieldSupertype,
                names = names,
                typeSystem = typeSystem
            )
            val messageTypeField = fld.createClass()
            addLast(messageTypeField)
        }
    }
}

/**
 * Converts this string to [ClassName] by parsing its value.
 *
 * The function assumes that package names start with a lowercase letter, and
 * class names start with an uppercase letter.
 */
private fun String.toClassName(): ClassName {
    val packageSeparator = "."
    val items = split(packageSeparator)
    val packageName = items.filter { it[0].isLowerCase() }.joinToString(packageSeparator)
    val simpleNames = items.filter { it[0].isUpperCase() }
    return ClassName(packageName, simpleNames)
}
