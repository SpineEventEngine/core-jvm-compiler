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

import com.google.common.collect.ImmutableSet
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod
import io.spine.tools.compiler.ast.Field
import io.spine.tools.compiler.ast.MessageType
import io.spine.tools.compiler.ast.columns
import io.spine.tools.compiler.context.CodegenContext
import io.spine.tools.compiler.jvm.render.CreateNestedClass
import io.spine.tools.compiler.render.SourceFile
import io.spine.tools.code.Java
import io.spine.tools.core.jvm.GeneratedAnnotation
import io.spine.tools.core.jvm.entity.EntityPlugin.Companion.COLUMN_CLASS_NAME
import io.spine.tools.core.jvm.entity.EntityPlugin.Companion.DEFINITIONS_METHOD_NAME
import io.spine.tools.core.jvm.entity.TypeReferences
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
        val typeReferences = TypeReferences(
            file = psiFile,
            stateClass = messageClass,
            typeSystem = typeSystem
        )
        columns.forEach { column ->
            ColumnAccessor(
                entityState = messageClass,
                field = column,
                columnClass = cls,
                typeReferences = typeReferences
            ).run {
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

        /** The generic type that matches all the columns of this entity state. */
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

        /** The type that is returned by the method. */
        private val resultSet: String = ImmutableSet::class.java.reference

        /** The piece of method body that adds columns to [accumulator]. */
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
