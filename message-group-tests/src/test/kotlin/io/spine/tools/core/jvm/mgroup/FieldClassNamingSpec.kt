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
import io.spine.tools.core.jvm.PluginTestSetup
import io.spine.tools.core.jvm.settings.GroupSettings
import java.nio.file.Path
import kotlin.io.path.Path
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Tests that the classes generated for the types of the fields of a message have
 * different names, and that field accessors return these classes.
 *
 * Please see the `given/naming` directory for the types used in the tests.
 */
@DisplayName("Field classes with clashing names should")
internal class FieldClassNamingSpec {

    companion object : PluginTestSetup<GroupSettings>(
        MessageGroupPlugin(),
        MessageGroupPlugin.SETTINGS_ID
    ) {
        val fieldSupertype: String = EventMessageField::class.java.canonicalName

        @BeforeAll
        @JvmStatic
        fun setup(@TempDir projectDir: Path) {
            runPipeline(projectDir)
        }

        override fun createSettings(projectDir: Path): GroupSettings {
            val codegenConfig = createCompilerSettings(projectDir)
            listOf("home.Contact", "home.Room", "home.CrateField", "outer.Cargo").forEach { name ->
                codegenConfig.forMessage("given.naming.$name") {
                    it.markFieldsAs(fieldSupertype)
                }
            }
            return codegenConfig.toProto().groupSettings
        }

        fun code(fileName: String): String =
            file(Path("io/spine/given/naming/$fileName.java")).code()
    }

    private fun String.shouldDeclare(fieldClass: String) {
        this shouldContain "public static final class $fieldClass extends $fieldSupertype {"
    }

    @Test
    fun `be told apart by the package if their types have the same name`() {
        code("home/Contact").run {
            shouldDeclare("AddressField")
            shouldDeclare("WebAddressField")
            this shouldContain "public static AddressField address() {"
            this shouldContain "public static WebAddressField page() {"
        }
    }

    @Test
    fun `be told apart by an underscore if their types are from one package`() {
        code("home/Room").run {
            shouldDeclare("ShelfBoxField")
            shouldDeclare("Shelf_BoxField")
            this shouldContain "public static ShelfBoxField spare() {"
            this shouldContain "public Shelf_BoxField box() {"
        }
    }

    @Test
    fun `have an underscore before the suffix if the name is taken by the message`() {
        code("home/CrateField").run {
            shouldDeclare("Crate_Field")
            this shouldContain "public static Crate_Field crate() {"
        }
    }

    @Test
    fun `have an underscore before the suffix if the name is taken by the outer class`() {
        code("outer/CrateField").run {
            shouldDeclare("Crate_Field")
            this shouldContain "public static Crate_Field crate() {"
        }
    }
}
