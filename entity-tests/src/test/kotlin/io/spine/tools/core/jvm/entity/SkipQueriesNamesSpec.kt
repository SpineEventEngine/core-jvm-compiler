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

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.spine.testing.compiler.acceptingOnly
import io.spine.tools.core.jvm.entity.EntityPlugin.Companion.COLUMN_CLASS_NAME
import io.spine.tools.core.jvm.entity.EntityPlugin.Companion.QUERY_BUILDER_CLASS_NAME
import io.spine.tools.core.jvm.entity.EntityPluginTestSetup.Companion.java
import io.spine.tools.core.jvm.entity.given.reserved.Board
import io.spine.tools.core.jvm.gradle.settings.EntitySettings
import io.spine.tools.core.jvm.settings.Entities
import java.nio.file.Path
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Tests that no names are reserved if the support of queries is turned off.
 *
 * @see ReservedNamesSpec
 */
@DisplayName("`EntityPlugin` should")
internal class SkipQueriesNamesSpec {

    companion object : EntityPluginTestSetup() {

        /**
         * Creates the settings with the generation of queries turned off.
         */
        override fun createSettings(projectDir: Path): Entities =
            EntitySettings(createProject(projectDir))
                .apply { skipQueries() }
                .toProto()
    }

    @Test
    fun `accept a nested message with a reserved name if 'skipQueries()' is set`(
        @TempDir projectDir: Path
    ) {
        // With the default settings, this type is rejected. See `ReservedMessageNameErrorSpec`.
        shouldNotThrowAny {
            runPipeline(projectDir, acceptingOnly(Board.getDescriptor()))
        }

        file("reserved/Board".java).code().let {
            // The class of the nested message.
            it shouldContain "class $COLUMN_CLASS_NAME "
            // The class is not generated because queries are turned off in `createSettings()`.
            it shouldNotContain "class $QUERY_BUILDER_CLASS_NAME"
        }
    }
}
