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
import io.spine.testing.compiler.acceptingOnly
import io.spine.testing.compiler.assertCompilationError
import io.spine.tools.core.jvm.entity.EntityPlugin.Companion.QUERY_BUILDER_CLASS_NAME
import io.spine.tools.core.jvm.entity.given.reserved.QueryBuilder
import io.spine.tools.core.jvm.placeOf
import java.nio.file.Path
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Tests that an entity state cannot have a reserved name.
 *
 * @see ReservedNamesSpec
 */
@DisplayName("`QueryBuilderClass` should")
internal class ReservedStateNameErrorSpec {

    companion object : EntityPluginTestSetup()

    @Test
    fun `reject an entity state with the same name`(@TempDir projectDir: Path) {
        val state = QueryBuilder.getDescriptor()
        val (error, _) = assertCompilationError {
            runPipeline(projectDir, acceptingOnly(state))
        }
        error.message.let {
            it shouldContain state.placeOf("message QueryBuilder {")
            it shouldContain "The message `${state.fullName}`" +
                    " cannot have the name `$QUERY_BUILDER_CLASS_NAME`."
            it shouldContain "reserved for the class which the compiler generates"
            it shouldContain "for the entity state `${state.fullName}`"
            it shouldContain "Please rename the message."
        }
    }
}
