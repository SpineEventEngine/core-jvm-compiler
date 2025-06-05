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
import com.intellij.psi.javadoc.PsiDocComment
import io.spine.protodata.java.ClassName
import io.spine.query.IdCriterion
import io.spine.tools.java.reference
import io.spine.tools.psi.addFirst
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.addLast
import org.intellij.lang.annotations.Language

/**
 * Generates the method for restricting query results to certain entity identifiers.
 *
 * @param queryBuilderClass
 *         the class in which to generate the method.
 * @param methodName
 *         the name of the method to generate.
 * @param idType
 *         the name of entity state identifier type.
 */
internal class IdMethod(
    private val entityStateClass: ClassName,
    private val queryBuilderClass: PsiClass,
    private val methodName: String,
    private val idType: String,
) {
    private val javadoc: PsiDocComment by lazy {
        val entityStateType = entityStateClass.simpleName
        @Language("JAVA") @Suppress("EmptyClass")
        val doc = elementFactory.createDocCommentFromText("""
            /**
             * Creates a criterion for the identifier of the {@link $entityStateType} entity state.
             */
            """.trimIndent()
        )
        doc
    }

    private val idCriterion: String = IdCriterion::class.java.reference

    @Language("JAVA") @Suppress("EmptyClass")
    private val returnType: String = "$idCriterion<$idType, ${queryBuilderClass.name}>"

    private val method by lazy {
        @Language("JAVA") @Suppress("EmptyClass", "NewClassNamingConvention")
        val newMethod = elementFactory.createMethodFromText("""
            public $returnType $methodName() {
                return new $idCriterion<>(this);
            }            
            """.trimIndent(), queryBuilderClass
        )
        newMethod.addFirst(javadoc)
        newMethod
    }

    /**
     * Adds the method to [queryBuilderClass].
     */
    fun render() {
        queryBuilderClass.addLast(method)
    }
}
