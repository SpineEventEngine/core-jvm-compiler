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

package io.spine.tools.core.jvm.entity.query

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import io.spine.protodata.ast.MessageType
import io.spine.protodata.ast.columns
import io.spine.protodata.context.CodegenContext
import io.spine.protodata.java.javaCase
import io.spine.protodata.render.SourceFile
import io.spine.query.EntityQueryBuilder
import io.spine.tools.code.Java
import io.spine.tools.core.jvm.entity.EntityPlugin.Companion.QUERY_BUILDER_CLASS_NAME
import io.spine.tools.core.jvm.entity.EntityPlugin.Companion.QUERY_CLASS_NAME
import io.spine.tools.core.jvm.settings.Entities
import io.spine.tools.java.reference
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.createClassReference
import io.spine.tools.psi.java.setSuperclass
import org.intellij.lang.annotations.Language

/**
 * Creates a [QueryBuilder][QUERY_BUILDER_CLASS_NAME] class nested under an entity state class
 * to allow creating typed queries for the state of an [Entity][io.spine.server.entity.Entity].
 *
 * Builds a DSL specific to the declared entity [columns][io.spine.query.EntityColumn].
 *
 * ## Example
 *
 * Consider the following proto definition:
 *
 * ```proto
 *   message Customer {
 *       option (entity).kind = PROJECTION;
 *
 *       CustomerId id = 1;
 *
 *       string name = 2 [(required) = true];
 *
 *       EmailAddress email = 3;
 *
 *       Address address = 4;
 *
 *       CustomerType type = 5 [(required) = true, (column) = true];
 *
 *       int32 discount_percent = 6 [(min).value = "0", (column) = true];
 *   }
 * ```
 * Taking the above definition, this generator would produce DSL for building a query:
 *
 * ```java
 *     Customer.query()
 *             .id().in(westCustomerIds())
 *             .type().is("permanent")    // `type()` is a `...Criterion`.
 *             .discountPercent().isGreaterThan(10)
 *             .sortAscendingBy(Column.name())
 *             .withMask(Field.name(), Field.address())  // `Customer.Field` type is generated.
 *             .limit(1)
 *             .build()     // `Customer.Query`
 * ```
 * @see QueryClass
 * @see io.spine.tools.core.jvm.entity.column.AddColumnClass
 * @see io.spine.tools.core.jvm.field.AddFieldClass
 */
internal class QueryBuilderClass(
    type: MessageType,
    file: SourceFile<Java>,
    settings: Entities,
    context: CodegenContext
) : QuerySupportClass(type, file, QUERY_BUILDER_CLASS_NAME, settings, context) {

    /**
     * The value used for brevity when referencing in the generated code.
     */
    private val query = QUERY_CLASS_NAME

    @Language("JAVA") @Suppress("EmptyClass")
    override fun classJavadoc(): String = """
        /**
         * A builder for the queries for the {@link $stateType} entity state.
         * 
         * @see $query
         */       
        """.trimIndent()

    /**
     * Creates the private constructor which calls `super()` with two parameters:
     *  1. The class of the entity state identifiers.
     *  2. The entity state class.
     */
    override fun createConstructor(cls: PsiClass): PsiMethod {
        val ctor = elementFactory.createMethodFromText("""
            private $simpleName() {
                super($idType.class, $stateType.class);
            }
            """.trimIndent(), cls
        )
        return ctor
    }

    override fun tuneClass() {
        cls.run {
            extendEntityQueryBuilder()
            addIdMethod()
            addColumnsMethod()
            addThisRefMethod()
            addBuildMethod()
        }
    }

    /**
     * Makes the class extend [EntityQueryBuilder].
     *
     * The generic parameters are:
     * 1) The type of the [first][idField] entity state type field.
     * 2) The type of the entity state.
     * 3) The type of this query builder class.
     * 4) The type of the query class.
     */
    private fun PsiClass.extendEntityQueryBuilder() {
        val superClass = elementFactory.createClassReference(
            EntityQueryBuilder::class.java.reference,
            // Generic parameters:
            idType, stateType, this.name!!, query,
            context = cls
        )
        setSuperclass(superClass)
    }

    private fun PsiClass.addIdMethod() {
        val idMethod = IdMethod(
            entityStateClass,
            queryBuilderClass = this,
            methodName = idField.name.javaCase(),
            idType,
        )
        idMethod.render()
    }

    private fun PsiClass.addColumnsMethod() {
        val columns = type.columns
        columns.forEach {
            val column = QueryColumn(entityStateClass, it, queryBuilder = this, typeSystem)
            column.render()
        }
    }

    private fun PsiClass.addThisRefMethod() =
        ThisRefMethod(queryBuilder = this).run {
            render()
        }

    private fun PsiClass.addBuildMethod() =
        BuildMethod(queryBuilder = this).run {
            render()
        }
}
