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

package io.spine.tools.mc.java.entity.field

import com.intellij.psi.PsiJavaFile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.spine.string.Indent.Companion.defaultJavaIndent
import io.spine.tools.java.reference
import io.spine.tools.mc.java.entity.EntityPluginTest
import io.spine.tools.mc.java.entity.assertHasMethod
import io.spine.tools.mc.java.entity.innerClass
import io.spine.tools.mc.java.field.FieldClass.Companion.NAME
import io.spine.tools.psi.java.locate
import java.nio.file.Path
import javax.annotation.Generated
import kotlin.io.path.Path
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("Generated `Field` class should")
internal class AddFieldClassSpec : EntityPluginTest() {

    companion object {

        private const val ENTITY_STATE = "Department"

        lateinit var entityStateCode: String
        private lateinit var psiFile: PsiJavaFile

        @BeforeAll
        @JvmStatic
        fun setup(
            @TempDir projectDir: Path,
            @TempDir outputDir: Path,
            @TempDir settingsDir: Path
        ) {
            val sourceFileSet = runPipeline(projectDir, outputDir, settingsDir)
            val sourceFile = sourceFileSet.file(Path(DEPARTMENT_JAVA))
            entityStateCode = sourceFile.code()
            psiFile = sourceFile.psi() as PsiJavaFile
        }

        fun fieldClass() = psiFile.locate(ENTITY_STATE, NAME)
    }

    @Test
    fun `be 'public', 'static', and 'final'`() {
        val decl = defaultJavaIndent.toString() + "public static final class $NAME"
        entityStateCode shouldContain decl
    }

    @Test
    fun `nested under the entity state class`() {
        fieldClass() shouldNotBe null
    }

    @Test
    fun `provide methods for accessing fields`() {
        val fieldClass = fieldClass()!!
        fieldClass.run {
            assertHasMethod("key")
            assertHasMethod("name")
            assertHasMethod("description")
            assertHasMethod("manager")
            assertHasMethod("staff")
        }
    }

    @Test
    fun `provide nested classes for fields with message types`() {
        val fieldClass = fieldClass()!!
        fieldClass.innerClass("DepartmentKeyField").run {
            assertHasMethod("uuid")
        }
        fieldClass.innerClass("EmployeeField").run {
            assertHasMethod("id")
            assertHasMethod("name")
        }
    }

    @Test
    fun `be annotated as 'Generated'`() {
        fieldClass()!!.run {
            annotations.size shouldBe 1
            annotations[0].qualifiedName shouldBe Generated::class.java.reference
        }
    }
}
