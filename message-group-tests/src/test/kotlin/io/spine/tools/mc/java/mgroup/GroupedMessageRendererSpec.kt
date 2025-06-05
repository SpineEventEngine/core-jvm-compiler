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
import io.spine.tools.mc.java.mgroup.given.CustomField
import io.spine.tools.mc.java.mgroup.given.StudentIdClass
import io.spine.tools.mc.java.settings.GroupSettings
import java.nio.file.Path
import kotlin.io.path.Path
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`GroupedMessageRenderer` should")
internal class GroupedMessageRendererSpec {

    companion object : PluginTestSetup<GroupSettings>(
        MessageGroupPlugin(),
        MessageGroupPlugin.SETTINGS_ID
    ) {
        lateinit var code: String

        @BeforeAll
        @JvmStatic
        fun setup(@TempDir projectDir: Path) {
            val settings = createSettings(projectDir)
            val setup = setup(projectDir, settings)
            val pipeline = setup.createPipeline()
            pipeline()
            val sourceFileSet = pipeline.sources[0]
            code = sourceFileSet.find(
                Path("io/spine/tools/mc/mgroup/given/Student.java")
            )!!.code()
        }

        override fun createSettings(projectDir: Path): GroupSettings {
            val codegenConfig = createCodegenConfig(projectDir)
            codegenConfig.forMessage("given.groups.Student") {
                it.markFieldsAs(CustomField::class.java.canonicalName)
                it.useAction(StudentIdClass::class.java.canonicalName)
            }
            return codegenConfig.toProto().groupSettings
        }
    }

    @Test
    fun `render 'Field' class`() {
        code shouldContain "public static final class Field"
    }

    @Test
    fun `render a nested class using a renderer`() {
        code shouldContain "public static final class ${StudentIdClass.CLASS_NAME}"
    }
}
