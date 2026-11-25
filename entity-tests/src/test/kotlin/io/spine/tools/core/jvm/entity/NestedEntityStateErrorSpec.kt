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

import com.google.protobuf.Descriptors.Descriptor
import io.kotest.matchers.string.shouldContain
import io.spine.testing.compiler.acceptingOnly
import io.spine.testing.compiler.assertCompilationError
import io.spine.tools.compiler.Compilation
import io.spine.tools.core.jvm.entity.given.Office
import java.nio.file.Path
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Tests that an entity state nested in another message fails compilation.
 *
 * @see DeeplyNestedEntityStateErrorSpec
 */
@DisplayName("`EntityDiscovery` should")
internal class NestedEntityStateErrorSpec {

    companion object : EntityPluginTestSetup()

    @Test
    fun `reject an entity state nested in another message`(@TempDir projectDir: Path) {
        val (error, _) = assertCompilationError {
            runPipeline(projectDir, acceptingOnly(Office.getDescriptor()))
        }
        error.shouldReject(Office.Room.getDescriptor())
    }
}

/**
 * Asserts that this error is about the given nested entity [state], and that it refers to
 * the place in the proto file at which the [state] is declared.
 */
internal fun Compilation.Error.shouldReject(state: Descriptor) {
    val (line, column) = state.findDeclaration()
    message.let {
        it shouldContain "${state.file.name}:$line:$column: "
        it shouldContain "The `(entity)` option cannot be applied"
        it shouldContain "the message `${state.fullName}`"
        it shouldContain "nested in `${state.containingType.fullName}`"
        it shouldContain "must be a top-level message"
        it shouldContain "move `${state.name}` to the top level"
    }
}

/**
 * Finds the line and the column, both one-based, at which this message is declared.
 *
 * We search in the text of the proto file because the descriptors available from
 * the generated code do not have the information on the source code.
 */
private fun Descriptor.findDeclaration(): Pair<Int, Int> {
    val classLoader = NestedEntityStateErrorSpec::class.java.classLoader
    val protoFile = checkNotNull(classLoader.getResource(file.name)) {
        "Unable to find the resource `${file.name}`."
    }
    val declaration = "message $name {"
    val (index, line) = protoFile.readText().lines()
        .withIndex()
        .first { (_, line) -> declaration in line }
    return Pair(index + 1, line.indexOf(declaration) + 1)
}
