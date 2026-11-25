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

package io.spine.tools.core.jvm.field

import io.kotest.matchers.string.shouldContain
import io.spine.base.EventMessageField
import io.spine.tools.compiler.jvm.ClassName
import io.spine.tools.compiler.protobuf.toField
import io.spine.tools.core.jvm.field.given.farmField
import io.spine.tools.core.jvm.field.given.farmFieldClassNames
import io.spine.tools.core.jvm.field.given.farmTypeSystem
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`FieldAccessor` should")
internal class FieldAccessorSpec {

    private val supertype = ClassName(EventMessageField::class.java)
    private val typeSystem = farmTypeSystem()

    private fun topLevel(fieldName: String) = TopLevelFieldAccessor(
        field = farmField(fieldName).toField(),
        fieldSupertype = supertype,
        names = farmFieldClassNames(),
        typeSystem = typeSystem
    )

    private fun nested(fieldName: String) = NestedFieldAccessor(
        field = farmField(fieldName).toField(),
        fieldSupertype = supertype,
        names = farmFieldClassNames(),
        typeSystem = typeSystem
    )

    @Nested inner class
    `generate a top-level accessor` {

        @Test
        fun `for a simple field`() {
            val method = topLevel("name").method()
            val text = method.text
            text shouldContain "public static"
            text shouldContain supertype.canonical
            text shouldContain "name()"
            text shouldContain "io.spine.base.Field.named(\"name\")"
            text shouldContain "Returns the {@code name} field."
            text shouldContain "field Java type is {@code String}"
        }

        @Test
        fun `for a message-typed field exposing nested fields`() {
            val method = topLevel("barn").method()
            val text = method.text
            text shouldContain "BarnField barn()"
            text shouldContain "new BarnField("
        }

        @Test
        fun `for a repeated field`() {
            val text = topLevel("tags").method().text
            text shouldContain "Returns the {@code repeated} {@code tags} field."
            text shouldContain "element Java type"
        }

        @Test
        fun `for a map field`() {
            val text = topLevel("names_by_id").method().text
            text shouldContain "Returns the {@code map} {@code names_by_id} field."
            text shouldContain "value Java type"
        }
    }

    @Nested inner class
    `generate a nested field accessor` {

        @Test
        fun `with an instance modifier and chained field path`() {
            val method = nested("name").method()
            val text = method.text
            text shouldContain "public "
            text shouldContain "getField().nested(\"name\")"
        }
    }
}
