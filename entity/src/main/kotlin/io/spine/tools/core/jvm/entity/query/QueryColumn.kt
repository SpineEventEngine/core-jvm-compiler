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

package io.spine.tools.core.jvm.entity.query

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.javadoc.PsiDocComment
import io.spine.tools.compiler.ast.Field
import io.spine.tools.compiler.jvm.ClassName
import io.spine.tools.compiler.jvm.typeReference
import io.spine.tools.compiler.type.TypeSystem
import io.spine.query.EntityCriterion
import io.spine.tools.core.jvm.entity.EntityPlugin.Companion.COLUMN_CLASS_NAME
import io.spine.tools.core.jvm.entity.column.columnMethodName
import io.spine.tools.java.reference
import io.spine.tools.psi.addFirst
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.addLast
import org.intellij.lang.annotations.Language

/**
 * Generates the method that produces a column criterion for
 * [EntityQueryBuilder][io.spine.query.EntityQueryBuilder] to restrict the value of the column.
 */
internal class QueryColumn(
    private val entityState: ClassName,
    private val field: Field,
    private val queryBuilder: PsiClass,
    private val typeSystem: TypeSystem
) {

    /**
     * The name of the method for accessing the column.
     */
    private val method: String = columnMethodName(field)

    /**
     * The name of the column class, as a local value for brevity at usage sites.
     */
    private val column = COLUMN_CLASS_NAME

    private val javadoc: PsiDocComment by lazy {
        @Language("JAVA") @Suppress("EmptyClass")
        val doc = elementFactory.createDocCommentFromText("""
            /**
             * Creates a criterion for the {@link $column#$method() $column.$method()} column.
             */           
            """.trimIndent(), queryBuilder
        )
        doc
    }

    private val fieldType by lazy {
        field.typeReference(entityState, typeSystem)
    }

    private val entityCriterion: String = EntityCriterion::class.java.reference

    private val returnType: String by lazy {
        "$entityCriterion<${entityState.simpleName}, $fieldType, ${queryBuilder.name}>"
    }

    /**
     * The code of the method.
     */
    private val result: PsiMethod by lazy {
        @Language("JAVA") @Suppress("EmptyClass", "NewClassNamingConvention")
        val newMethod = elementFactory.createMethodFromText("""
            public $returnType $method() {
                return new $entityCriterion<>($column.$method(), this);
            }                   
            """.trimIndent(), queryBuilder
        )
        newMethod.addFirst(javadoc)
        newMethod
    }

    /**
     * Renders the method in [queryBuilder].
     */
    fun render() {
        queryBuilder.addLast(result)
    }
}
