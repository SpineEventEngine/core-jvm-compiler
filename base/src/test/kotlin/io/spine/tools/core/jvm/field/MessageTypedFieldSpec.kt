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

import com.intellij.psi.PsiClass
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.spine.base.EventMessageField
import io.spine.tools.compiler.jvm.ClassName
import io.spine.tools.compiler.protobuf.toMessageType
import io.spine.tools.core.jvm.field.given.farmFieldClassNames
import io.spine.tools.core.jvm.field.given.farmTypeSystem
import io.spine.tools.core.jvm.given.base.Barn
import io.spine.tools.psi.java.execute
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`MessageTypedField` should")
internal class MessageTypedFieldSpec {

    private val supertype = ClassName(EventMessageField::class.java)
    private val typeSystem = farmTypeSystem()

    @Test
    fun `generate a class for a message-typed field`() {
        val type = Barn.getDescriptor().toMessageType()
        lateinit var cls: PsiClass
        execute {
            cls = MessageTypedField(
                fieldType = type,
                fieldSupertype = supertype,
                names = farmFieldClassNames(),
                typeSystem = typeSystem
            ).createClass()
        }

        cls.name shouldBe "BarnField"
        cls.hasModifierProperty("public").shouldBeTrue()
        cls.hasModifierProperty("static").shouldBeTrue()
        cls.hasModifierProperty("final").shouldBeTrue()

        val text = cls.text
        text shouldContain supertype.canonical
        text shouldContain "private BarnField(io.spine.base.Field field)"
        text shouldContain "super(field);"
        // Methods for the fields of `Barn`.
        text shouldContain "title()"
        text shouldContain "stall()"
        // The Javadoc mentioning the message type.
        text shouldContain "Provides fields of the {@link " +
                "io.spine.tools.core.jvm.given.base.Barn} message type."
    }
}
