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
 * This test suite supplements [GroupedMessageRendererSpec] suite to test code generation
 * of the type similar to [io.spine.core.EventContext] from the `core-java` module.
 *
 * Please see the `given.core` proto package for details.
 */
@DisplayName("Code generation of `oneof` should")
internal class OneofFieldCodegenSpec {

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
            val file = file(Path("io/spine/given/core/EventContext.java"))
            code = file.code()
        }

        /**
         * Mimics codegen settings declared in `core-java/code/build.gradle.kts`.
         */
        override fun createSettings(projectDir: Path): GroupSettings {
            val codegenConfig = createCompilerSettings(projectDir)
            codegenConfig.forMessage("given.core.EventContext") {
                it.markFieldsAs(FIELD_SUPERTYPE)
            }
            return codegenConfig.toProto().groupSettings
        }
    }

    /**
     * Tests that a top-level field and a field under `oneof` which has the same type
     * also has the field accessor returning the same type.
     */
    @Test
    fun `expose accessors for nested message fields`() {
        // Check the accessor for the top-level field.
        code shouldContain "public static CommandIdField rootCommandId() {"

        // Check the accessor for the field under `oneof`.
        code shouldContain "public static CommandIdField commandId() {"
    }

    @Test
    fun `generate field classes for nested message fields`() {
        code shouldContain
                "public static final class CommandIdField extends $FIELD_SUPERTYPE {"
        code shouldContain
                "public static final class EventIdField extends $FIELD_SUPERTYPE {"
    }
}
