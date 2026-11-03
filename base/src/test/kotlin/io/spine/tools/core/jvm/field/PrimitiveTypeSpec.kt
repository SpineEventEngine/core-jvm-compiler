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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.optional.shouldBeEmpty
import io.kotest.matchers.optional.shouldBePresent
import io.kotest.matchers.shouldBe
import io.spine.tools.compiler.jvm.JavaTypeName
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`PrimitiveType` should")
internal class PrimitiveTypeSpec {

    @Test
    fun `provide wrappers for all supported primitives`() {
        PrimitiveType.wrapperFor("int").shouldBePresent() shouldBe Int::class.javaObjectType
        PrimitiveType.wrapperFor("long").shouldBePresent() shouldBe Long::class.javaObjectType
        PrimitiveType.wrapperFor("float").shouldBePresent() shouldBe Float::class.javaObjectType
        PrimitiveType.wrapperFor("double").shouldBePresent() shouldBe Double::class.javaObjectType
        PrimitiveType.wrapperFor("boolean").shouldBePresent() shouldBe Boolean::class.javaObjectType
    }

    @Test
    fun `return empty wrapper for a non-primitive name`() {
        PrimitiveType.wrapperFor("String").shouldBeEmpty()
    }

    @Test
    fun `reject an empty type name`() {
        shouldThrow<IllegalArgumentException> {
            PrimitiveType.wrapperFor("")
        }
    }

    @Test
    fun `provide type names for all supported primitives`() {
        PrimitiveType.typeNameFor("int").shouldBePresent() shouldBe JavaTypeName.INT
        PrimitiveType.typeNameFor("long").shouldBePresent() shouldBe JavaTypeName.LONG
        PrimitiveType.typeNameFor("float").shouldBePresent() shouldBe JavaTypeName.FLOAT
        PrimitiveType.typeNameFor("double").shouldBePresent() shouldBe JavaTypeName.DOUBLE
        PrimitiveType.typeNameFor("boolean").shouldBePresent() shouldBe JavaTypeName.BOOLEAN
    }

    @Test
    fun `return empty type name for a name not representing a Protobuf scalar`() {
        PrimitiveType.typeNameFor("char").shouldBeEmpty()
        PrimitiveType.typeNameFor("String").shouldBeEmpty()
    }
}
