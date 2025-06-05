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

package io.spine.tools.mc.java.entity.query

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.javadoc.PsiDocComment
import io.spine.protodata.ast.Field
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.typeReference
import io.spine.protodata.type.TypeSystem
import io.spine.query.EntityCriterion
import io.spine.tools.java.reference
import io.spine.tools.mc.java.entity.EntityPlugin.Companion.COLUMN_CLASS_NAME
import io.spine.tools.mc.java.entity.column.columnMethodName
import io.spine.tools.psi.addFirst
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.addLast
import org.intellij.lang.annotations.Language

/**
 * Generates the method which produces a column criterion for
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
