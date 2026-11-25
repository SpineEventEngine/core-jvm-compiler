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

import com.intellij.psi.PsiJavaFile
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.refactoring.extractMethod.newImpl.ExtractMethodHelper.addSiblingAfter
import io.spine.logging.WithLogging
import io.spine.tools.compiler.render.SourceFile
import io.spine.tools.code.Java
import io.spine.tools.core.jvm.GeneratedAnnotation
import io.spine.tools.core.jvm.entity.EntityPlugin.Companion.QUERY_BUILDER_CLASS_NAME
import io.spine.tools.core.jvm.entity.EntityPlugin.Companion.QUERY_METHOD_NAME
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
