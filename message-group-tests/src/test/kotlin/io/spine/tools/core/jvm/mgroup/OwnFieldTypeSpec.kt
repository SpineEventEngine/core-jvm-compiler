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
import io.kotest.matchers.string.shouldNotContain
import io.spine.base.EventMessageField
import io.spine.logging.testing.tapConsole
import io.spine.tools.core.jvm.PluginTestSetup
import io.spine.tools.core.jvm.settings.GroupSettings
import java.nio.file.Path
import kotlin.io.path.Path
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Tests the name of the class generated for the fields of a message if the name `Field`
 * is already taken.
 *
 * Please see the `given/naming` directory for the types used in the tests.
 */
@DisplayName("The class for the fields of a message should")
internal class OwnFieldTypeSpec {

    companion object : PluginTestSetup<GroupSettings>(
        MessageGroupPlugin(),
        MessageGroupPlugin.SETTINGS_ID
    ) {
        private val messages = listOf(
            "home.Contact",
            "home.Form",
            "home.Survey",
            "home.Field",
            "field.Plot"
        )

        lateinit var output: String

        @BeforeAll
        @JvmStatic
        fun setup(@TempDir projectDir: Path) {
            output = tapConsole {
                runPipeline(projectDir)
            }
        }

        override fun createSettings(projectDir: Path): GroupSettings {
            val codegenConfig = createCompilerSettings(projectDir)
            messages.forEach { name ->
                codegenConfig.forMessage("given.naming.$name") {
                    it.markFieldsAs(EventMessageField::class.java.canonicalName)
                }
            }
            return codegenConfig.toProto().groupSettings
        }

        fun code(fileName: String): String =
            file(Path("io/spine/given/naming/$fileName.java")).code()
    }

    @Test
    fun `have the usual name if it is not taken`() {
        code("home/Contact") shouldContain "public static final class Field {"
    }

    @Test
    fun `have an underscore if there is a nested message 'Field'`() {
        code("home/Form").run {
            this shouldContain "public static final class Field_ {"
            this shouldNotContain "public static final class Field {"
            this shouldContain "public static FormFieldField first() {"
        }
    }

    @Test
    fun `have an underscore if there is a nested enum 'Field'`() {
        code("home/Survey") shouldContain "public static final class Field_ {"
    }

    @Test
    fun `have an underscore if the message is named 'Field'`() {
        code("home/Field") shouldContain "public static final class Field_ {"
    }

    @Test
    fun `have an underscore if the outer class is named 'Field'`() {
        code("field/Field") shouldContain "public static final class Field_ {"
    }

    @Test
    fun `be mentioned in a warning only if it has an underscore`() {
        (messages - "home.Contact").forEach {
            output shouldContain "the message type `given.naming.$it` is generated as `Field_`"
        }
        output shouldNotContain "the message type `given.naming.home.Contact`"
    }
}
