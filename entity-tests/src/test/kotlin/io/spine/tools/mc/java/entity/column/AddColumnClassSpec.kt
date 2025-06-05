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

package io.spine.tools.mc.java.entity.column

import com.intellij.psi.PsiJavaFile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.spine.annotation.Generated
import io.spine.string.Indent.Companion.defaultJavaIndent
import io.spine.tools.java.reference
import io.spine.tools.mc.java.entity.EntityPlugin.Companion.COLUMN_CLASS_NAME
import io.spine.tools.mc.java.entity.EntityPlugin.Companion.DEFINITIONS_METHOD_NAME
import io.spine.tools.mc.java.entity.EntityPluginTestSetup
import io.spine.tools.mc.java.entity.assertDoesNotHaveMethod
import io.spine.tools.mc.java.entity.assertHasMethod
import io.spine.tools.psi.java.locate
import java.nio.file.Path
import kotlin.io.path.Path
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`AddColumnClass` should")
internal class AddColumnClassSpec {

    companion object : EntityPluginTestSetup() {

        private const val ENTITY_STATE = "Department"

        lateinit var entityStateCode: String
        private lateinit var psiFile: PsiJavaFile

        @BeforeAll
        @JvmStatic
        fun setup(@TempDir projectDir: Path) {
            runPipeline(projectDir)
            val sourceFile = file(Path(DEPARTMENT_JAVA))
            entityStateCode = sourceFile.code()
            psiFile = sourceFile.psi() as PsiJavaFile
        }

        fun columnClass() = psiFile.locate(ENTITY_STATE, COLUMN_CLASS_NAME)
    }

    @Test
    fun `be 'public', 'static', and 'final'`() {
        val decl = defaultJavaIndent.toString() + "public static final class $COLUMN_CLASS_NAME"
        entityStateCode shouldContain decl
    }

    @Test
    fun `be nested under the entity state class`() {
        columnClass() shouldNotBe null
    }

    @Test
    fun `provide 'definitions' method`() {
        val methods = columnClass()!!.findMethodsByName(DEFINITIONS_METHOD_NAME)
        methods.size shouldBe 1
    }

    /**
     * Tests that the `Column` class has methods only for the marked fields.
     *
     * See also a similar test for generated `QueryBuilder` class.
     *
     * @see io.spine.tools.mc.java.entity.query.QueryBuilderClassSpec
     */
    @Test
    fun `expose methods for columns`() {
        val columnClass = columnClass()!!
        columnClass.run {
            // See that we have methods for columns.
            assertHasMethod("name")
            assertHasMethod("description")
            assertHasMethod("manager")

            // See that we don't have methods for other fields.
            assertDoesNotHaveMethod("key")
            assertDoesNotHaveMethod("staff")
        }
    }

    @Test
    fun `be annotated as 'Generated'`() {
        columnClass()!!.run {
            annotations.size shouldBe 1
            annotations[0].qualifiedName shouldBe Generated::class.java.reference
        }
    }
}
