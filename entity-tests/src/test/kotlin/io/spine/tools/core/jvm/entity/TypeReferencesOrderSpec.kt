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

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.spine.tools.compiler.render.copy
import io.spine.tools.core.jvm.entity.EntityPluginTestSetup.Companion.java
import io.spine.tools.core.jvm.entity.column.AddColumnClass
import io.spine.tools.core.jvm.field.AddFieldClass
import io.spine.tools.core.jvm.settings.Entities
import io.spine.tools.core.jvm.settings.copy
import io.spine.tools.java.reference
import java.nio.file.Path
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Tests that references to types do not depend on the order in which the classes are generated.
 *
 * @see TypeReferencesSpec
 */
@DisplayName("`TypeReferences` should")
internal class TypeReferencesOrderSpec {

    companion object : EntityPluginTestSetup() {

        /**
         * Creates the settings with the actions in the order reverse to the default one.
         */
        override fun createSettings(projectDir: Path): Entities {
            val settings = super.createSettings(projectDir)
            val reversed = settings.actions.actionMap.entries.reversed()
            return settings.copy {
                actions = actions.copy {
                    action.clear()
                    reversed.forEach { (name, parameter) -> action.put(name, parameter) }
                }
            }
        }
    }

    @Test
    fun `have the package if the class with the same name is generated later`(
        @TempDir projectDir: Path
    ) {
        val order = createSettings(projectDir).actions.actionMap.keys.toList()
        val columnFirst = order.indexOf(AddColumnClass::class.java.reference) <
                order.indexOf(AddFieldClass::class.java.reference)
        columnFirst shouldBe true

        runPipeline(projectDir)

        val code = file("references/Plot".java).code().filterNot { it.isWhitespace() }
        val type = "io.spine.tools.core.jvm.entity.given.references.Field"
        code shouldContain "EntityColumn<Plot,$type>fourth(){"
    }
}
