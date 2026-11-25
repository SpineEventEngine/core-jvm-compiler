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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.spine.tools.core.jvm.entity.EntityPlugin.Companion.COLUMN_CLASS_NAME
import io.spine.tools.core.jvm.entity.EntityPlugin.Companion.QUERY_BUILDER_CLASS_NAME
import io.spine.tools.core.jvm.entity.EntityPlugin.Companion.QUERY_CLASS_NAME
import io.spine.tools.psi.java.topLevelClass
import java.nio.file.Path
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Tests which types may have the names reserved for the classes generated for entity states.
 *
 * Please see `given/entities/reserved_names.proto` for the types used in the tests.
 *
 * @see ReservedMessageNameErrorSpec
 * @see ReservedEnumNameErrorSpec
 * @see ReservedStateNameErrorSpec
 * @see SkipQueriesNamesSpec
 */
@DisplayName("`EntityDiscovery` should")
internal class ReservedNamesSpec {

    companion object : EntityPluginTestSetup() {

        @BeforeAll
        @JvmStatic
        fun setup(@TempDir projectDir: Path) {
            runPipeline(projectDir)
        }

        private fun classOf(typeName: String): PsiClass {
            val psiFile = file("reserved/$typeName".java).psi() as PsiJavaFile
            return psiFile.topLevelClass
        }
    }

    @Test
    fun `accept an entity state with the reserved names nested deeper than one level`() {
        val state = classOf("Cabinet")
        listOf(COLUMN_CLASS_NAME, QUERY_CLASS_NAME, QUERY_BUILDER_CLASS_NAME).forEach {
            state.findInnerClassByName(it, false) shouldNotBe null
        }
    }

    @Test
    fun `accept the reserved names in a message which is not an entity state`() {
        val message = classOf("ReadBoard")
        message.findInnerClassByName(COLUMN_CLASS_NAME, false) shouldNotBe null
        message.findInnerClassByName(QUERY_CLASS_NAME, false) shouldNotBe null
        message.findInnerClassByName(QUERY_BUILDER_CLASS_NAME, false) shouldBe null
    }
}
