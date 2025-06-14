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
import io.spine.protodata.context.CodegenContext
import io.spine.protodata.render.SourceFile
import io.spine.query.EntityQuery
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
 * Creates a [Query][QUERY_CLASS_NAME] class nested under an entity state class.
 *
 * @see QueryBuilderClass
 */
internal class QueryClass(
    type: MessageType,
    file: SourceFile<Java>,
    settings: Entities,
    context: CodegenContext
) : QuerySupportClass(type, file, QUERY_CLASS_NAME, settings, context) {

    /**
     * The value used for brevity when referencing in the generated code.
     */
    private val queryBuilder = QUERY_BUILDER_CLASS_NAME

    @Language("JAVA") @Suppress("EmptyClass")
    override fun classJavadoc(): String = """
        /**
         * A query for finding entity states of the type {@link $stateType}.
         *
         * @see $stateType#query  
         * @see $queryBuilder
         */
        """.trimIndent()

    /**
     * Creates the constructor for the query class which accepts
     * an instance of `QueryBuilder` as the parameter.
     */
    override fun createConstructor(cls: PsiClass): PsiMethod {
        val ctor = elementFactory.createMethodFromText("""
            private ${cls.name}($queryBuilder builder) {
                super(builder);                
            }            
            """.trimIndent(), cls
        )
        return ctor
    }

    /**
     * Makes the class extend [EntityQuery].
     *
     * The generic parameters are:
     * 1) The type of the [first][idField] entity state type field.
     * 2) The type of the entity state.
     * 3) The generated [QueryBuilder][QUERY_BUILDER_CLASS_NAME] class.
     */
    override fun tuneClass() {
        val superClass = elementFactory.createClassReference(
            EntityQuery::class.java.reference,
            // Generic parameters:
            idType, stateType, queryBuilder,
            context = cls
        )
        cls.setSuperclass(superClass)
    }
}
