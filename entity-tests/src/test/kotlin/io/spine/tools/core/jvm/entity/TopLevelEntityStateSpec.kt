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

package io.spine.tools.core.jvm.entity

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.spine.tools.core.jvm.entity.EntityPlugin.Companion.COLUMN_CLASS_NAME
import io.spine.tools.core.jvm.entity.EntityPlugin.Companion.QUERY_BUILDER_CLASS_NAME
import io.spine.tools.core.jvm.entity.EntityPlugin.Companion.QUERY_CLASS_NAME
import io.spine.tools.core.jvm.entity.EntityPlugin.Companion.QUERY_METHOD_NAME
import io.spine.tools.psi.java.topLevelClass
import java.nio.file.Path
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Tests that the ban on nesting applies only to entity states.
 *
 * A top-level entity state can have other types nested in it.
 */
@DisplayName("A top-level entity state with nested types should")
internal class TopLevelEntityStateSpec {

    companion object : EntityPluginTestSetup() {

        /**
         * The names of the classes generated for an entity state for supporting queries.
         */
        private val generatedClasses = listOf(
            COLUMN_CLASS_NAME,
            QUERY_CLASS_NAME,
            QUERY_BUILDER_CLASS_NAME
        )

        private lateinit var stateClass: PsiClass

        @BeforeAll
        @JvmStatic
        fun setup(@TempDir projectDir: Path) {
            runPipeline(projectDir)
            val psiFile = file("Warehouse".java).psi() as PsiJavaFile
            stateClass = psiFile.topLevelClass
        }
    }

    @Test
    fun `have the query API`() {
        stateClass.assertHasMethod(QUERY_METHOD_NAME)
        stateClass.innerClassNames() shouldContainAll generatedClasses
    }

    @Test
    fun `have columns of nested types`() {
        stateClass.innerClass(COLUMN_CLASS_NAME).run {
            assertHasMethod("status")
            assertHasMethod("frontShelf")
        }
    }

    @Test
    fun `not have the query API in the nested message`() {
        val nested = stateClass.innerClass("Shelf")
        nested.assertDoesNotHaveMethod(QUERY_METHOD_NAME)
        nested.innerClassNames() shouldNotContainAnyOf generatedClasses
    }
}

private fun PsiClass.innerClassNames(): List<String> = innerClasses.mapNotNull { it.name }
