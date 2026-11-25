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
 * Tests that an entity state nested several levels deep fails compilation.
 *
 * It lives in its own class because each suite runs the pipeline only once
 * (see the case of one level in `NestedEntityStateErrorSpec`).
 */
@DisplayName("`EntityDiscovery` should")
internal class DeeplyNestedEntityStateErrorSpec {

    companion object : EntityPluginTestSetup()

    @Test
    fun `reject an entity state nested several levels deep`(@TempDir projectDir: Path) {
        val (error, _) = assertCompilationError {
            runPipeline(projectDir, acceptingOnly(Campus.getDescriptor()))
        }
        error.shouldReject(Campus.Building.Floor.getDescriptor())
    }
}
