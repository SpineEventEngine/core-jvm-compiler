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

import io.kotest.matchers.string.shouldContain
import java.nio.file.Path
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Tests how the code generated for an entity state refers to the types of columns and IDs.
 *
 * Please see `given/entities/references.proto` and `given/entities/enum_references.proto` for
 * the types used in the tests.
 */
@DisplayName("`TypeReferences` should")
internal class TypeReferencesSpec {

    companion object : EntityPluginTestSetup() {

        private const val PACKAGE = "io.spine.tools.core.jvm.entity.given.references"

        @BeforeAll
        @JvmStatic
        fun setup(@TempDir projectDir: Path) {
            runPipeline(projectDir)
        }

        /**
         * Obtains the code generated for the type without whitespace so that the assertions do
         * not depend on the formatting.
         */
        private fun code(typeName: String): String =
            file("references/$typeName".java).code().filterNot { it.isWhitespace() }

        /**
         * Asserts that this code, generated for a state called `Plot`, refers to the type of
         * the [column] as [type].
         */
        private fun String.assertColumn(column: String, type: String) {
            val getter = "get" + column.replaceFirstChar { it.uppercase() }
            this shouldContain "EntityColumn<Plot,$type>$column(){"
            this shouldContain "EntityColumn<>(\"$column\",$type.class,Plot::$getter)"
            this shouldContain "EntityCriterion<Plot,$type,QueryBuilder>$column(){"
        }

        /**
         * Asserts that the code generated for the [state] refers to the type of the ID as [type].
         */
        private fun assertId(state: String, type: String) {
            code(state).let {
                it shouldContain "EntityQueryBuilder<$type,$state,QueryBuilder,Query>{"
                it shouldContain "super($type.class,$state.class);"
                it shouldContain "IdCriterion<$type,QueryBuilder>id(){"
                it shouldContain "EntityQuery<$type,$state,QueryBuilder>{"
            }
        }
    }

    @ParameterizedTest(name = "{1}")
    @CsvSource(
        "first, Column",
        "second, Query",
        "third, QueryBuilder",
        "fourth, Field",
        "fifth, Status",
        "sixth, SourceCase",
        "ninth, Status.Detail"
    )
    fun `have the package if the type of a column is named as a nested class`(
        column: String,
        type: String
    ) {
        code("Plot").assertColumn(column, "$PACKAGE.$type")
    }

    @ParameterizedTest(name = "{1}")
    @CsvSource(
        "first, Column",
        "second, Query",
        "third, QueryBuilder",
        "fourth, Field"
    )
    fun `have the package if the enum type of a column is named as a nested class`(
        column: String,
        type: String
    ) {
        code("enums/Plot").assertColumn(column, "$PACKAGE.enums.$type")
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource("Column", "Query", "QueryBuilder", "Field", "Status")
    fun `have the package if the type of the ID is named as a nested class`(type: String) {
        assertId(state = "${type}Holder", type = "$PACKAGE.$type")
    }

    @Test
    fun `be short if no nested class has the name of the type`() {
        code("Plot").run {
            assertColumn("seventh", "Address")
            assertColumn("tenth", "Field_")
        }
        code("enums/Plot").assertColumn("fifth", "Color")
        assertId(state = "AddressHolder", type = "Address")
    }

    @Test
    fun `be short if the type is nested in the entity state`() {
        code("Plot").assertColumn("eighth", "Status")
    }
}
