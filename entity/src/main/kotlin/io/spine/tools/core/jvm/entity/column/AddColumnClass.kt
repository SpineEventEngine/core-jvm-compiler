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

package io.spine.tools.core.jvm.entity.column

import com.google.common.collect.ImmutableSet
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.MessageType
import io.spine.protodata.ast.columns
import io.spine.protodata.context.CodegenContext
import io.spine.protodata.java.render.CreateNestedClass
import io.spine.protodata.render.SourceFile
import io.spine.tools.code.Java
import io.spine.tools.core.jvm.GeneratedAnnotation
import io.spine.tools.core.jvm.entity.EntityPlugin.Companion.COLUMN_CLASS_NAME
import io.spine.tools.core.jvm.entity.EntityPlugin.Companion.DEFINITIONS_METHOD_NAME
import io.spine.tools.java.reference
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.addLast
import java.lang.String.format
import org.intellij.lang.annotations.Language

/**
 * Creates a class called [Column][COLUMN_CLASS_NAME] and nests it under an entity state class.
 *
 * The class provides API for obtaining columns for given `EntityState` [type].
 * The `Column` class is `public static` and stateless.
 * It serves as a DSL for calling `public static` methods for obtaining
 * entity state [columns][io.spine.query.EntityColumn].
 *
 * Since the `Column` class is not meant to be instantiated, a private parameterless
 * constructor is generated.
 *
 * In addition to methods for obtaining individual columns, a [method][DEFINITIONS_METHOD_NAME]
 * for obtaining all the columns is also generated.
 *
 * @param type the type of the `EntityState` message.
 * @param file the file to which the action is applied.
 * @param context the code generation context in which this action runs.
 *
 * @see render
 */
public class AddColumnClass(type: MessageType, file: SourceFile<Java>, context: CodegenContext) :
    CreateNestedClass(type, file, COLUMN_CLASS_NAME, context) {

    private val columns: List<Field> = type.columns

    override fun createAnnotation(): PsiAnnotation = GeneratedAnnotation.forPsi()

    @Language("JAVA") @Suppress("EmptyClass")
    override fun classJavadoc(): String = """
        /**
         * A listing of entity columns defined in $messageJavadocRef.
         *
         * <p>Use static methods of this class to access the columns of the entity
         * which can then be used for creating filters in a query.
         */
        """.trimIndent()

    override fun tuneClass() {
        addColumnMethods()
        addDefinitionsMethod()
    }

    private fun addColumnMethods() {
        columns.forEach { column ->
            ColumnAccessor(messageClass, column, cls, typeSystem).run {
                render()
            }
        }
    }

    private fun addDefinitionsMethod() {
        val method = DefinitionsMethod().create()
        cls.addLast(method)
    }

    /**
     * Method object for creating [definitions][DEFINITIONS_METHOD_NAME] method.
     */
    private inner class DefinitionsMethod {

        /** The generic type which matches all the columns of this entity state. */
        private val columnWildcard = columnType(messageClass)

        /**
         * The name of the variable to collect all column definitions
         *
         * We use `buildString` instead of a plain literal to avoid the `Missing identifier`
         * warning in IDEA.
         */
        private val accumulator: String = buildString {
            append("columns")
        }

        /** The type which is returned by the method. */
        private val resultSet: String = ImmutableSet::class.java.reference

        /** The piece of method body which adds columns to [accumulator]. */
        private val addingColumns: String by lazy {
            columns
                .map { column -> columnMethodName(column) }
                .joinToString(separator = "\n  ") { method ->
                    "$accumulator.add($method());"
                }
        }

        /** Builds the full text of the method. */
        private val methodText: String by lazy {
            @Language("JAVA") @Suppress("EmptyClass", "DanglingJavadoc", "NewClassNamingConvention")
            val methodTemplate = """
            /**
             * Returns all the column definitions of $messageJavadocRef.
             */
            public static $resultSet<$columnWildcard> $DEFINITIONS_METHOD_NAME() {
              var $accumulator = new java.util.HashSet<$columnWildcard>();
              %s
              return $resultSet.copyOf($accumulator);
            }                                
            """.trimIndent()
            format(methodTemplate, addingColumns)
        }

        fun create(): PsiMethod {
            val method = elementFactory.createMethodFromText(methodText, cls)
            return method
        }
    }
}
