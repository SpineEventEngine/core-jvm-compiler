/*
 * Copyright 2026, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.tools.core.jvm.field

import com.intellij.psi.PsiClass
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.spine.base.EventMessageField
import io.spine.tools.compiler.jvm.ClassName
import io.spine.tools.compiler.protobuf.toMessageType
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

    @Test
    fun `compose a name for a nested message type`() {
        val nested = Barn.Stall.getDescriptor().toMessageType()
        MessageTypedField.classNameFor(nested) shouldBe "BarnStallField"
    }
}
