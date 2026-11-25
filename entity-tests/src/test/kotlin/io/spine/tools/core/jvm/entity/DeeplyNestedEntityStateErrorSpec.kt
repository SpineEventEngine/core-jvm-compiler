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

import io.spine.testing.compiler.acceptingOnly
import io.spine.testing.compiler.assertCompilationError
import io.spine.tools.core.jvm.entity.given.Campus
import java.nio.file.Path
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Tests that nesting an entity state several levels deep fails compilation.
 *
 * The suite is separate from [NestedEntityStateErrorSpec], which covers one level of nesting,
 * because the pipeline can run only once per test class.
 */
@DisplayName("`EntityDiscovery` should")
internal class DeeplyNestedEntityStateErrorSpec {

    companion object : EntityPluginTestSetup()

    @Test
    fun `reject an entity state nested several levels deep`(@TempDir projectDir: Path) {
        val (error, _) = assertCompilationError {
            runPipeline(projectDir, acceptingOnly(Campus.getDescriptor()))
        }
        error.shouldReportNested(Campus.Building.Floor.getDescriptor())
    }
}
