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

package io.spine.tools.core.jvm.mgroup

import io.kotest.matchers.string.shouldContain
import io.spine.tools.core.jvm.PluginTestSetup
import io.spine.tools.core.jvm.settings.GroupSettings
import java.nio.file.Path
import kotlin.io.path.Path
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Tests the names of field classes generated for the message types that are used as
 * the types of fields, including the types nested in other message types.
 *
 * Please see the `given/groups/nested_types.proto` file for the types used in the tests.
 */
@DisplayName("Field classes for message types should")
internal class NestedTypeFieldSpec {

    companion object : PluginTestSetup<GroupSettings>(
        MessageGroupPlugin(),
        MessageGroupPlugin.SETTINGS_ID
    ) {
        const val FIELD_SUPERTYPE = "io.spine.core.EventContextField"

        lateinit var code: String

        @BeforeAll
        @JvmStatic
        fun setup(@TempDir projectDir: Path) {
            runPipeline(projectDir)
            val file = file(Path("io/spine/tools/core/mgroup/given/Enrollment.java"))
            code = file.code()
        }

        override fun createSettings(projectDir: Path): GroupSettings {
            val codegenConfig = createCompilerSettings(projectDir)
            codegenConfig.forMessage("given.groups.Enrollment") {
                it.markFieldsAs(FIELD_SUPERTYPE)
            }
            return codegenConfig.toProto().groupSettings
        }
    }

    @Test
    fun `be named after a message type`() {
        code shouldContain
                "public static final class CourseField extends $FIELD_SUPERTYPE {"
    }

    @Test
    fun `be named after the outer type and a message type nested in it`() {
        code shouldContain
                "public static final class CourseScheduleField extends $FIELD_SUPERTYPE {"
    }
}
