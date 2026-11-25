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
import io.spine.base.EventMessageField
import io.spine.given.naming.home.Form
import io.spine.testing.compiler.assertCompilationError
import io.spine.tools.core.jvm.PluginTestSetup
import io.spine.tools.core.jvm.placeOf
import io.spine.tools.core.jvm.settings.GroupSettings
import java.nio.file.Path
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Tests that the compilation fails if the name of the class generated for the fields of
 * a message belongs to a type nested in this message.
 *
 * Please see the `given/naming/home.proto` file for the type used in the test.
 */
@DisplayName("`AddFieldClass` should")
internal class ReservedFieldNameErrorSpec {

    companion object : PluginTestSetup<GroupSettings>(
        MessageGroupPlugin(),
        MessageGroupPlugin.SETTINGS_ID
    ) {
        override fun createSettings(projectDir: Path): GroupSettings {
            val codegenConfig = createCompilerSettings(projectDir)
            codegenConfig.forMessage(Form.getDescriptor().fullName) {
                it.markFieldsAs(EventMessageField::class.java.canonicalName)
            }
            return codegenConfig.toProto().groupSettings
        }
    }

    @Test
    fun `reject a nested message named 'Field'`(@TempDir projectDir: Path) {
        val (error, _) = assertCompilationError {
            runPipeline(projectDir)
        }
        val nested = Form.Field.getDescriptor()
        error.message.let {
            it shouldContain nested.placeOf("message Field {")
            it shouldContain "The message `${nested.fullName}` cannot have the name `Field`."
            it shouldContain "for the message `${Form.getDescriptor().fullName}`."
        }
    }
}
