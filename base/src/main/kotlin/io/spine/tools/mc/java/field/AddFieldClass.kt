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

package io.spine.tools.mc.java.field

import com.google.protobuf.StringValue
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import io.spine.protodata.ast.Cardinality.CARDINALITY_SINGLE
import io.spine.protodata.ast.MessageType
import io.spine.protodata.ast.MessageTypeDependencies
import io.spine.protodata.context.CodegenContext
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.render.CreateNestedClass
import io.spine.protodata.render.SourceFile
import io.spine.tools.code.Java
import io.spine.tools.mc.java.GeneratedAnnotation
import io.spine.tools.mc.java.field.AddFieldClass.Companion.NAME
import io.spine.tools.psi.java.addLast
import org.intellij.lang.annotations.Language

/**
 * Creates a nested class called [`Field`][NAME] under a Java class generated for
 * the given message [type].
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
) : CreateNestedClass(type, file, NAME, context) {

    private val fieldSupertype: ClassName = fieldSuperClassName.value.toClassName()

    public companion object {

        /**
         * The name of the generated Java class.
         */
        public const val NAME: String = "Field"
    }

    override fun createAnnotation(): PsiAnnotation = GeneratedAnnotation.forPsi()

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
            val accessor = TopLevelFieldAccessor(it, fieldSupertype, typeSystem)
            addLast(accessor.method())
        }
    }

    private fun PsiClass.addFieldClasses() {
        val deps = MessageTypeDependencies(type, CARDINALITY_SINGLE, typeSystem).asSet()
        deps.forEach {
            val fld = MessageTypedField(it, fieldSupertype, typeSystem)
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
