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

package io.spine.tools.core.jvm.signal.rejection

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Declarations of the generated code should")
internal class DeclarationsSpec {

    @Nested inner class
    `compose a method` {

        @Test
        fun `indenting its statements`() {
            methodDeclaration(
                signature = "void run()",
                statements = listOf("first();", "second();")
            ) shouldBe """
                void run() {
                    first();
                    second();
                }
                """.trimIndent()
        }

        @Test
        fun `with an empty body`() {
            methodDeclaration(
                signature = "private Builder()",
                statements = emptyList()
            ) shouldBe "private Builder() {\n}"
        }

        @Test
        fun `preceded by its Javadoc`() {
            methodDeclaration(
                javadoc = "The summary.\n\n@return nothing\n",
                signature = "void run()",
                statements = emptyList()
            ) shouldBe """
                /**
                 * The summary.
                 *
                 * @return nothing
                 */
                void run() {
                }
                """.trimIndent()
        }

        @Test
        fun `accepting any line break in its Javadoc`() {
            methodDeclaration(
                javadoc = "The summary.\r\n\r\nThe details.\r\n",
                signature = "void run()",
                statements = emptyList()
            ) shouldBe methodDeclaration(
                javadoc = "The summary.\n\nThe details.\n",
                signature = "void run()",
                statements = emptyList()
            )
        }
    }

    @Test
    fun `compose a class with its members separated by blank lines`() {
        val field = "private int count;"
        val method = methodDeclaration(
            javadoc = "Runs.\n",
            signature = "void run()",
            statements = listOf("count++;")
        )
        classDeclaration(
            javadoc = "The class.\n",
            header = "public class Counter",
            members = listOf(field, method)
        ) shouldBe """
            /**
             * The class.
             */
            public class Counter {
                private int count;

                /**
                 * Runs.
                 */
                void run() {
                    count++;
                }
            }
            """.trimIndent()
    }
}
