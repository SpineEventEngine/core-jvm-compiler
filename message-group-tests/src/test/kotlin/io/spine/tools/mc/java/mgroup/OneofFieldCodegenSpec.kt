/*
 * Copyright 2025, TeamDev. All rights reserved.
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

package io.spine.tools.mc.java.mgroup

import io.kotest.matchers.string.shouldContain
import io.spine.tools.mc.java.PluginTestSetup
import io.spine.tools.mc.java.settings.GroupSettings
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
            val codegenConfig = createCodegenConfig(projectDir)
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

    @Test
    fun `generate field classes for nested message types`() {
        code shouldContain
                "public static final class EnrichmentContainerField extends $FIELD_SUPERTYPE {"
    }
}
