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

package io.spine.tools.mc.java.entity.query

import com.intellij.psi.PsiJavaFile
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.refactoring.extractMethod.newImpl.ExtractMethodHelper.addSiblingAfter
import io.spine.logging.WithLogging
import io.spine.protodata.render.SourceFile
import io.spine.tools.code.Java
import io.spine.tools.mc.java.GeneratedAnnotation
import io.spine.tools.mc.java.entity.EntityPlugin.Companion.QUERY_BUILDER_CLASS_NAME
import io.spine.tools.mc.java.entity.EntityPlugin.Companion.QUERY_METHOD_NAME
import io.spine.tools.psi.addFirst
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.topLevelClass
import org.intellij.lang.annotations.Language

/**
 * Renders the `query()` method at the top level Java class of the given file.
 *
 * The method is added after the constructors.
 * This makes it more visible to the people who dare to look at the generated code.
 */
internal class QueryMethod(private val file: SourceFile<Java>) : WithLogging {

    private val psiFile = file.psi() as PsiJavaFile
    private val entityStateClass = psiFile.topLevelClass
    private val queryBuilder = QUERY_BUILDER_CLASS_NAME

    private val javadoc: PsiDocComment by lazy {
        @Language("JAVA") @Suppress("EmptyClass")
        val doc = elementFactory.createDocCommentFromText("""
            /**
             * Creates a new instance of {@link QueryBuilder}.
             */
            """.trimIndent()
        )
        doc
    }

    /**
     * The value used for brevity when referencing in the generated code.
     */
    private val methodName = QUERY_METHOD_NAME

    private val method by lazy {
        @Language("JAVA") @Suppress("EmptyClass", "NewClassNamingConvention")
        val newMethod = elementFactory.createMethodFromText("""
            public static $queryBuilder $methodName() {
                return new $queryBuilder();
            }            
            """.trimIndent(), entityStateClass
        )
        newMethod.run {
            val annotation = GeneratedAnnotation.forPsi()
            addFirst(annotation)
            addFirst(javadoc)
        }
        newMethod
    }

    /**
     * Renders the `query()` method placing it after the last constructor.
     */
    @Suppress("TooGenericExceptionCaught") // ... to log diagnostic.
    fun render() {
        try {
            val lastConstructor = entityStateClass.constructors.last()
            lastConstructor.addSiblingAfter(method)

            val updatedFile = psiFile.text
            file.overwrite(updatedFile)
        } catch (e: Throwable) {
            logger.atError().withCause(e).log { """
                Caught exception while rendering the `query()` method in `${entityStateClass.name}`.
                Message: ${e.message}.                
                """.trimIndent()
            }
        }
    }
}
