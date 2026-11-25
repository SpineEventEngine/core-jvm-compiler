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

package io.spine.tools.core.jvm

import com.google.protobuf.Descriptors.Descriptor
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.string.shouldContain
import io.spine.testing.compiler.assertCompilationError
import io.spine.tools.compiler.protobuf.ProtoFileList
import io.spine.tools.compiler.protobuf.toMessageType
import io.spine.tools.compiler.protobuf.toPbSourceFile
import io.spine.tools.compiler.type.TypeSystem
import io.spine.tools.core.jvm.given.base.reserved.Field
import io.spine.tools.core.jvm.given.base.reserved.Form
import io.spine.tools.core.jvm.given.base.reserved.Garden
import io.spine.tools.core.jvm.given.base.reserved.Survey
import io.spine.tools.core.jvm.given.base.reserved.outer.Field.Plot
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests the rule on the names reserved for the classes nested in the Java classes of messages.
 *
 * Please see `given/base/reserved_names.proto` and `given/base/reserved_outer_class.proto` for
 * the types used in the tests.
 */
@DisplayName("`ReservedName` should")
internal class ReservedNameSpec {

    @Test
    fun `accept a message with other names`() {
        shouldNotThrowAny {
            check(Garden.getDescriptor())
        }
    }

    @Test
    fun `tell what the name is reserved for`() {
        val error = errorOf(Form.getDescriptor(), ownerKind = "entity state")
        error shouldContain "This name is reserved for the class which the compiler generates" +
                " for the entity state `given.base.reserved.Form`."
    }

    @Nested inner class
    `reject the name if it belongs to` {

        @Test
        fun `a nested message`() {
            val error = errorOf(Form.getDescriptor())
            error shouldContain "The message `given.base.reserved.Form.Field`" +
                    " cannot have the name `Field`."
            error shouldContain "for the message `given.base.reserved.Form`."
            error shouldContain "Please rename the message."
        }

        @Test
        fun `a nested enum`() {
            val error = errorOf(Survey.getDescriptor())
            error shouldContain "The enum `given.base.reserved.Survey.Field`" +
                    " cannot have the name `Field`."
            error shouldContain "for the message `given.base.reserved.Survey`."
            error shouldContain "Please rename the enum."
        }

        @Test
        fun `the message itself`() {
            val error = errorOf(Field.getDescriptor())
            error shouldContain "The message `given.base.reserved.Field`" +
                    " cannot have the name `Field`."
            error shouldContain "for the message `given.base.reserved.Field`."
            error shouldContain "Please rename the message."
        }

        @Test
        fun `a message around`() {
            val error = errorOf(Field.Part.getDescriptor())
            error shouldContain "The message `given.base.reserved.Field`" +
                    " cannot have the name `Field`."
            error shouldContain "for the message `given.base.reserved.Field.Part`."
        }

        @Test
        fun `a message several levels around`() {
            val error = errorOf(Field.Part.Row.getDescriptor())
            error shouldContain "The message `given.base.reserved.Field`" +
                    " cannot have the name `Field`."
            error shouldContain "for the message `given.base.reserved.Field.Part.Row`."
        }

        @Test
        fun `the outer class of the file`() {
            val error = errorOf(Plot.getDescriptor())
            error shouldContain "The outer Java class of the file cannot have the name `Field`."
            error shouldContain "for the message `given.base.reserved.outer.Plot`."
            error shouldContain "Please change the value of the `java_outer_classname` option."
        }

        @Test
        fun `the outer class of the file, and the message is nested`() {
            val error = errorOf(Plot.Row.getDescriptor())
            error shouldContain "The outer Java class of the file cannot have the name `Field`."
            error shouldContain "for the message `given.base.reserved.outer.Plot.Row`."
        }
    }
}

private fun errorOf(owner: Descriptor, ownerKind: String = "message"): String {
    val (error, _) = assertCompilationError {
        check(owner, ownerKind)
    }
    return checkNotNull(error.message)
}

private fun check(owner: Descriptor, ownerKind: String = "message") {
    val typeSystem = TypeSystem(
        ProtoFileList(emptyList()),
        setOf(owner.file.toPbSourceFile())
    )
    val name = ReservedName(
        value = "Field",
        owner = owner.toMessageType(),
        ownerKind = ownerKind,
        typeSystem = typeSystem
    )
    name.check()
}
