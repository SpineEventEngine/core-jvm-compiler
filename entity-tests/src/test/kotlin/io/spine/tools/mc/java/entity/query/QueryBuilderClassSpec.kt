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

import com.intellij.lang.jvm.JvmModifier.PROTECTED
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.spine.annotation.Generated
import io.spine.tools.java.reference
import io.spine.tools.mc.java.entity.EntityPlugin.Companion.BUILD_METHOD_NAME
import io.spine.tools.mc.java.entity.EntityPlugin.Companion.QUERY_BUILDER_CLASS_NAME
import io.spine.tools.mc.java.entity.EntityPlugin.Companion.THIS_REF_METHOD_NAME
import io.spine.tools.mc.java.entity.EntityPluginTestSetup
import io.spine.tools.mc.java.entity.assertDoesNotHaveMethod
import io.spine.tools.mc.java.entity.assertHasMethod
import io.spine.tools.psi.java.isPublic
import io.spine.tools.psi.java.method
import io.spine.tools.psi.java.topLevelClass
import java.nio.file.Path
import kotlin.io.path.Path
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.io.TempDir

@DisplayName("`QueryBuilderClass` should")
internal class QueryBuilderClassSpec {

    companion object : EntityPluginTestSetup() {

        private lateinit var entityStateClass: PsiClass

        @BeforeAll
        @JvmStatic
        fun setup(@TempDir projectDir: Path) {
            runPipeline(projectDir)
            val sourceFile = file(Path(DEPARTMENT_JAVA))
            val psiFile = sourceFile.psi() as PsiJavaFile
            entityStateClass = psiFile.topLevelClass
        }

        fun queryBuilderClass(): PsiClass? =
            entityStateClass.findInnerClassByName(QUERY_BUILDER_CLASS_NAME, false)
    }

    @Test
    fun `be nested under the entity state`() {
        queryBuilderClass() shouldNotBe null
    }

    @Test
    fun `be annotated as 'Generated'`() {
        queryBuilderClass()!!.run {
            annotations.size shouldBe 1
            annotations[0].qualifiedName shouldBe Generated::class.java.reference
        }
    }

    @Test
    fun `have the method for working with entity state IDs`() {
        queryBuilderClass()!!.assertHasMethod("key")
    }

    /**
     * Tests that the `QueryBuilder` class has methods only for the marked fields.
     *
     * See also a similar test for generated `Column` class.
     *
     * @see io.spine.tools.mc.java.entity.column.AddColumnClassSpec
     */
    @Test
    fun `expose methods for columns`() {
        queryBuilderClass()!!.run {
            // See that we have methods for columns.
            assertHasMethod("name")
            assertHasMethod("description")
            assertHasMethod("manager")

            // See that we don't have methods for other fields.
            assertDoesNotHaveMethod("staff")
        }
    }

    @Test
    fun `implement 'thisRef' method`() {
        val method = assertDoesNotThrow {
            queryBuilderClass()!!.method(THIS_REF_METHOD_NAME)
        }
        method.run {
            annotations.size shouldBe 1
            annotations[0].qualifiedName shouldBe Override::class.java.reference
            hasModifier(PROTECTED) shouldBe true
        }
    }

    @Test
    fun `implement 'build' method`() {
        val method = assertDoesNotThrow {
            queryBuilderClass()!!.method(BUILD_METHOD_NAME)
        }
        method.run {
            annotations.size shouldBe 1
            annotations[0].qualifiedName shouldBe Override::class.java.reference
            isPublic shouldBe true
            docComment shouldNotBe null
        }
    }
}
