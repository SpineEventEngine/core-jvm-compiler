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
import io.spine.tools.core.jvm.entity.given.DefaultSingleFileProto.Garage
import java.nio.file.Path
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Tests that declaring an entity state in a file which does not declare
 * the `java_multiple_files` option fails compilation.
 *
 * The suite is separate from [SingleFileEntityStateErrorSpec], which covers the option
 * set to `false`, because the pipeline can run only once per test class.
 */
@DisplayName("`EntityDiscovery` should")
internal class DefaultSingleFileEntityStateErrorSpec {

    companion object : EntityPluginTestSetup()

    @Test
    fun `reject an entity state if 'java_multiple_files' is not set`(@TempDir projectDir: Path) {
        val descriptor = Garage.getDescriptor()
        val (error, _) = assertCompilationError {
            runPipeline(projectDir, acceptingOnly(descriptor))
        }
        error.shouldReportSingleFile(descriptor)
    }
}
