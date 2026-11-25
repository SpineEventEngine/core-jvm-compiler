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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.spine.testing.compiler.assertCompilationError
import io.spine.tools.compiler.ast.TypeName
import io.spine.tools.compiler.ast.file
import io.spine.tools.compiler.ast.messageType
import io.spine.tools.compiler.ast.typeName
import io.spine.tools.compiler.jvm.ClassName
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`FieldClassNames` should")
internal class FieldClassNamesSpec {

    @Test
    fun `use short names if they do not clash`() {
        namesOf("acme.home:Address", "acme.home:Barn.Stall", "acme.web:Barn.Stall.Trough") shouldBe
                listOf("AddressField", "BarnStallField", "BarnStallTroughField")
    }

    @Test
    fun `tell the names that differ only in case as different`() {
        namesOf("acme.home:A.B", "acme.home:Ab") shouldBe listOf("ABField", "AbField")
    }

    @Test
    fun `reject an unknown type`() {
        val names = fieldClassNames(listOf("acme.home:Address"))
        shouldThrow<IllegalStateException> {
            names.of(type("acme.home:Crate"))
        }
    }

    @Test
    fun `not depend on the order of types`() {
        val types = listOf(
            "acme.home:ShelfBox",
            "acme.web:ShelfBox",
            "acme.web:Shelf.Box",
            "acme.home.v1:Address",
            "acme.web.v1:Address",
            "geo:Address",
        )
        val expected = fieldClassNames(types)
        listOf(types.reversed(), types.sorted(), types.drop(3) + types.take(3)).forEach {
            val actual = fieldClassNames(it)
            types.map { t -> actual.of(type(t)) } shouldBe types.map { t -> expected.of(type(t)) }
        }
    }

    @Nested inner class
    `for types with the same name from different packages` {

        @Test
        fun `keep the short name for the type from the package of the message`() {
            namesOf("acme.home:Address", "acme.web:Address") shouldBe
                    listOf("AddressField", "WebAddressField")
        }

        @Test
        fun `lengthen all names if no type is from the package of the message`() {
            namesOf("acme.mail:Address", "acme.web:Address") shouldBe
                    listOf("MailAddressField", "WebAddressField")
        }

        @Test
        fun `treat a subpackage as another package`() {
            namesOf("acme.home:Address", "acme.home.v1:Address") shouldBe
                    listOf("AddressField", "V1AddressField")
        }

        @Test
        fun `add package words until the names differ`() {
            namesOf("acme.mail.v1:Address", "acme.web.v1:Address") shouldBe
                    listOf("MailV1AddressField", "WebV1AddressField")
        }

        @Test
        fun `add package words only to the names that still clash`() {
            namesOf("acme.mail.v1:Address", "acme.web.v1:Address", "geo:Address") shouldBe
                    listOf("MailV1AddressField", "WebV1AddressField", "GeoAddressField")
        }

        @Test
        fun `convert a package word to camel case`() {
            namesOf("acme.home:Address", "acme.web_site:Address") shouldBe
                    listOf("AddressField", "WebSiteAddressField")
        }

        @Test
        fun `avoid a longer name that is taken by another type`() {
            namesOf("acme.home:Address", "acme.web:Address", "acme.home:WebAddress") shouldBe
                    listOf("AddressField", "AcmeWebAddressField", "WebAddressField")
        }

        @Test
        fun `avoid longer names that differ only in case`() {
            namesOf("acme.webapi:Address", "geo.web_api:Address") shouldBe
                    listOf("AcmeWebapiAddressField", "GeoWebApiAddressField")
        }

        @Test
        fun `allow names that differ only in case if it is the only way`() {
            namesOf(
                "acme.home:Address",
                "acme.web:Address",
                "acme.home:Webaddress",
                "acme.home:AcmeWebAddress"
            ) shouldBe listOf(
                "AddressField",
                "WebAddressField",
                "WebaddressField",
                "AcmeWebAddressField"
            )
        }

        @Test
        fun `keep the short name for a type without a package if it is the only one`() {
            namesOf(":Address", "acme.web:Address") shouldBe
                    listOf("AddressField", "WebAddressField")
        }

        @Test
        fun `report types that cannot be told apart`() {
            val (error, _) = assertCompilationError {
                namesOf(":Address", "acme.home:Address")
            }
            error.message.run {
                this shouldContain "`acme.home.Contact`"
                this shouldContain "`Address`, `acme.home.Address`"
                this shouldContain "`AddressField`"
            }
        }
    }

    @Nested inner class
    `for types with the same name from one package` {

        @Test
        fun `keep the short name for the least nested type`() {
            namesOf("acme.home:ShelfBox", "acme.home:Shelf.Box") shouldBe
                    listOf("ShelfBoxField", "Shelf_BoxField")
        }

        @Test
        fun `separate names of all nested types with underscores`() {
            namesOf("acme.home:Barn.StallTrough", "acme.home:Barn.Stall.Trough") shouldBe
                    listOf("BarnStallTroughField", "Barn_Stall_TroughField")
        }

        @Test
        fun `use underscores for types that are nested equally deep`() {
            namesOf("acme.home:A.BC", "acme.home:AB.C") shouldBe
                    listOf("A_BCField", "AB_CField")
        }

        @Test
        fun `use underscores for all nested types if the least nested one is not single`() {
            namesOf("acme.home:A.BCD", "acme.home:AB.CD", "acme.home:A.B.CD") shouldBe
                    listOf("A_BCDField", "AB_CDField", "A_B_CDField")
        }

        @Test
        fun `add a package word if the name with an underscore is taken`() {
            namesOf("acme.home:ShelfBox", "acme.home:Shelf.Box", "acme.home:Shelf_Box") shouldBe
                    listOf("ShelfBoxField", "HomeShelf_BoxField", "Shelf_BoxField")
        }

        @Test
        fun `handle types from another package separately`() {
            namesOf("acme.home:ShelfBox", "acme.web:ShelfBox", "acme.web:Shelf.Box") shouldBe
                    listOf("ShelfBoxField", "WebShelfBoxField", "WebShelf_BoxField")
        }

        @Test
        fun `not give the short name up to a type from another package`() {
            namesOf("acme.home:A.BC", "acme.home:AB.C", "acme.web:ABC") shouldBe
                    listOf("A_BCField", "AB_CField", "WebABCField")
        }
    }

    @Nested inner class
    `for a class named like a class around it` {

        @Test
        fun `put an underscore before the suffix`() {
            namesOf("acme.home:Crate", around = listOf("CrateField")) shouldBe
                    listOf("Crate_Field")
        }

        @Test
        fun `put an underscore before the suffix for a nested type`() {
            namesOf("acme.home:Shelf.Box", around = listOf("ShelfBoxField")) shouldBe
                    listOf("ShelfBox_Field")
        }

        @Test
        fun `consider all the classes around`() {
            namesOf(
                "acme.home:Crate", "acme.home:Box", "acme.home:Lid",
                around = listOf("CrateField", "Cargo", "BoxField")
            ) shouldBe listOf("Crate_Field", "Box_Field", "LidField")
        }

        @Test
        fun `put an underscore before the suffix of a longer name`() {
            namesOf(
                "acme.home:Crate", "acme.web:Crate",
                around = listOf("WebCrateField")
            ) shouldBe listOf("CrateField", "WebCrate_Field")
        }

        @Test
        fun `report a name with an underscore that is taken by another type`() {
            val (error, _) = assertCompilationError {
                namesOf("acme.home:Crate", "acme.home:Crate_", around = listOf("CrateField"))
            }
            error.message.run {
                this shouldContain "`acme.home.Crate`, `acme.home.Crate_`"
                this shouldContain "`Crate_Field`"
            }
        }

        @Test
        fun `report a name with an underscore that is taken by a class around`() {
            val (error, _) = assertCompilationError {
                namesOf("acme.home:Crate", around = listOf("CrateField", "Crate_Field"))
            }
            error.message.run {
                this shouldContain "`acme.home.Crate`"
                this shouldContain "`Crate_Field`"
            }
        }
    }
}

/**
 * Obtains the names of the classes for the given types in the order of the types.
 *
 * @param types The types, such as `acme.home:Shelf.Box`.
 * @param around The simple names of the classes around the `Field` class.
 */
private fun namesOf(
    vararg types: String,
    around: List<String> = listOf("Contact")
): List<String> {
    val names = fieldClassNames(types.toList(), around)
    return types.map { names.of(type(it)) }
}

/**
 * Creates the names of field classes for the `acme.home.Contact` message.
 */
private fun fieldClassNames(
    types: List<String>,
    around: List<String> = listOf("Contact")
): FieldClassNames {
    val message = messageType {
        name = type("acme.home:Contact")
        file = file { path = "acme/home/contact.proto" }
    }
    val fieldClass = ClassName("io.acme.home", around + AddFieldClass.NAME)
    return FieldClassNames(
        messageType = message,
        fieldClass = fieldClass,
        fieldTypes = types.map { type(it) }.toSet()
    )
}

/**
 * Parses the type name, such as `acme.home:Shelf.Box`.
 */
private fun type(value: String): TypeName {
    val (pkg, path) = value.split(":")
    val names = path.split(".")
    return typeName {
        packageName = pkg
        nestingTypeName.addAll(names.dropLast(1))
        simpleName = names.last()
    }
}
