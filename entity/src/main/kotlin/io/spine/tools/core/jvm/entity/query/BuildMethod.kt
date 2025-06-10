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
import com.intellij.psi.javadoc.PsiDocComment
import io.spine.tools.core.jvm.entity.EntityPlugin.Companion.BUILD_METHOD_NAME
import io.spine.tools.core.jvm.entity.EntityPlugin.Companion.QUERY_CLASS_NAME
import io.spine.tools.psi.addFirst
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.addLast
import io.spine.tools.psi.java.annotateOverride
import org.intellij.lang.annotations.Language

/**
 * Generates a [build()][io.spine.query.EntityQueryBuilder.build] method for
 * the given [queryBuilder] class.
 */
internal class BuildMethod(private val queryBuilder: PsiClass) {

    /**
     * The value for readability at the usage sites.
     */
    private val query = QUERY_CLASS_NAME

    private val javadoc: PsiDocComment by lazy {
        @Language("JAVA") @Suppress("EmptyClass")
        val doc = elementFactory.createDocCommentFromText("""
            /**
             * Creates a new instance of {@link $query} on top of this {@code ${queryBuilder.name}}.
             */
            """.trimIndent())
        doc
    }

    private val method: PsiMethod by lazy {
        val build = BUILD_METHOD_NAME
        @Language("JAVA") @Suppress("EmptyClass", "NewClassNamingConvention")
        val newMethod =  elementFactory.createMethodFromText("""
            public $query $build() {
                return new $query(this);            
            }            
            """.trimIndent(), queryBuilder
        ).also {
            it.annotateOverride()
            it.addFirst(javadoc)
        }
        newMethod
    }

    /**
     * Adds the method to [queryBuilder].
     */
    fun render() {
        queryBuilder.addLast(method)
    }
}
