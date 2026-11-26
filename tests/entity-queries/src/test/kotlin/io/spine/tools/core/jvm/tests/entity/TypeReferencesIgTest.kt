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

package io.spine.tools.core.jvm.tests.entity

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.spine.query.EntityColumn
import io.spine.query.EntityQuery
import io.spine.tools.references.Column
import io.spine.tools.references.ColumnHolder
import io.spine.tools.references.Field
import io.spine.tools.references.FieldHolder
import io.spine.tools.references.Plot
import io.spine.tools.references.PlotKt
import io.spine.tools.references.Query
import io.spine.tools.references.QueryBuilder
import io.spine.tools.references.QueryBuilderHolder
import io.spine.tools.references.Status
import io.spine.tools.references.column
import io.spine.tools.references.field
import io.spine.tools.references.queryBuilder
import io.spine.tools.references.status
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests that the Java code generated for an entity state compiles, and that the query API works,
 * if the types of columns or IDs have the names of the classes nested in the class of the state.
 *
 * Please see `spine/tools/references/references.proto` for the types used in the tests.
 */
@DisplayName("Query API of an entity state should")
internal class TypeReferencesIgTest {

    @Nested inner class
    `have columns of a type named as` {

        @Test
        fun `the class 'Column'`() {
            val value = column { title = "main" }
            val query = Plot.query().layout().`is`(value).build()
            query shouldSelectBy value
            Plot.Column.layout() shouldHaveType Column::class.java
        }

        @Test
        fun `the class 'Query'`() {
            val query = Plot.query().frequency().`is`(Query.DAILY).build()
            query shouldSelectBy Query.DAILY
            Plot.Column.frequency() shouldHaveType Query::class.java
        }

        @Test
        fun `the class 'QueryBuilder'`() {
            val value = queryBuilder { title = "visual" }
            val query = Plot.query().builder().`is`(value).build()
            query shouldSelectBy value
            Plot.Column.builder() shouldHaveType QueryBuilder::class.java
        }

        @Test
        fun `the class 'Field'`() {
            val value = field { title = "green" }
            val query = Plot.query().field().`is`(value).build()
            query shouldSelectBy value
            Plot.Column.field() shouldHaveType Field::class.java
        }

        @Test
        fun `a nested message`() {
            val value = status { title = "open" }
            val query = Plot.query().status().`is`(value).build()
            query shouldSelectBy value
            Plot.Column.status() shouldHaveType Status::class.java
        }
    }

    @Test
    fun `have columns of a nested type`() {
        val value = PlotKt.status { text = "sold" }
        val query = Plot.query().ownStatus().`is`(value).build()
        query shouldSelectBy value
        Plot.Column.ownStatus() shouldHaveType Plot.Status::class.java
    }

    @Nested inner class
    `have the ID of a type named as` {

        @Test
        fun `the class 'Column'`() {
            val value = column { title = "main" }
            val query = ColumnHolder.query().id().`is`(value).build()
            query shouldHaveId value
        }

        @Test
        fun `the class 'QueryBuilder'`() {
            val value = queryBuilder { title = "visual" }
            val query = QueryBuilderHolder.query().id().`is`(value).build()
            query shouldHaveId value
        }

        @Test
        fun `the class 'Field'`() {
            val value = field { title = "green" }
            val query = FieldHolder.query().id().`is`(value).build()
            query shouldHaveId value
        }
    }
}

private infix fun EntityColumn<*, *>.shouldHaveType(expected: Class<*>) {
    type() shouldBe expected
}

/**
 * Asserts that the only parameter of this query has the given value.
 */
private infix fun EntityQuery<*, *, *>.shouldSelectBy(expected: Any) {
    val parameters = subject().predicate().parameters()
    parameters.map { it.value() } shouldContainExactly listOf(expected)
}

/**
 * Asserts that this query asks for the only entity with the given ID.
 */
private infix fun <I : Any> EntityQuery<I, *, *>.shouldHaveId(expected: I) {
    subject().id().values() shouldContainExactly listOf(expected)
}
