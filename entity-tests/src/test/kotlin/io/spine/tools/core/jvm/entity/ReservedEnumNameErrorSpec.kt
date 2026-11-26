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
import io.spine.tools.core.jvm.entity.EntityPlugin.Companion.QUERY_CLASS_NAME
import io.spine.tools.core.jvm.entity.given.reserved.Report
import io.spine.tools.core.jvm.placeOf
import java.nio.file.Path
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Tests that an enum with a reserved name cannot be nested in an entity state.
 *
 * @see ReservedNamesSpec
 */
@DisplayName("`QueryClass` should")
internal class ReservedEnumNameErrorSpec {

    companion object : EntityPluginTestSetup()

    @Test
    fun `reject a nested enum with the same name`(@TempDir projectDir: Path) {
        val state = Report.getDescriptor()
        val nested = Report.Query.getDescriptor()
        val (error, _) = assertCompilationError {
            runPipeline(projectDir, acceptingOnly(state))
        }
        error.message.let {
            it shouldContain nested.placeOf("enum Query {")
            it shouldContain "The enum `${nested.fullName}`" +
                    " cannot have the name `$QUERY_CLASS_NAME`."
            it shouldContain "reserved for the class which the compiler generates"
            it shouldContain "for the entity state `${state.fullName}`"
            it shouldContain "Please rename the enum."
        }
    }
}
