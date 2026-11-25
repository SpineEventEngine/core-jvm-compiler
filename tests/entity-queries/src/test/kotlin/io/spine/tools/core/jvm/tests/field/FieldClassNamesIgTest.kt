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

package io.spine.tools.core.jvm.tests.field

import io.kotest.matchers.shouldBe
import io.spine.base.SubscribableField
import io.spine.tools.naming.home.Contact
import io.spine.tools.naming.home.CrateField
import io.spine.tools.naming.home.Field
import io.spine.tools.naming.home.Form
import io.spine.tools.naming.home.Room
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests that the Java code generated for message types with clashing names compiles, and
 * that the generated classes give access to the fields.
 *
 * Please see the `spine/tools/naming` directory for the types used in the tests.
 */
@DisplayName("Classes generated for message fields should")
internal class FieldClassNamesIgTest {

    @Test
    fun `tell apart types with the same name from different packages`() {
        Contact.Field.address().street() shouldHavePath "address.street"
        Contact.Field.page().url() shouldHavePath "page.url"
    }

    @Test
    fun `tell apart types with the same name from one package`() {
        Room.Field.spare().code() shouldHavePath "spare.code"
        Room.Field.shelf().box().label() shouldHavePath "shelf.box.label"
    }

    @Test
    fun `differ from the class of the message`() {
        CrateField.Field.crate().weight() shouldHavePath "crate.weight"
    }

    @Test
    fun `be available if the message has its own type named 'Field'`() {
        Form.Field_.first().label() shouldHavePath "first.label"
    }

    @Test
    fun `be available if the message is named 'Field'`() {
        Field.Field_.crop().weight() shouldHavePath "crop.weight"
    }
}

private infix fun SubscribableField.shouldHavePath(expected: String) {
    field.toString() shouldBe expected
}
