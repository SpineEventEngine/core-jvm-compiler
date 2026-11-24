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

package io.spine.tools.core.jvm.signal

import com.google.protobuf.Descriptors.FileDescriptor
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.spine.base.EntityState
import io.spine.base.MessageFile
import io.spine.base.MessageFile.COMMANDS
import io.spine.base.MessageFile.EVENTS
import io.spine.base.MessageFile.REJECTIONS
import io.spine.code.proto.FileSet
import io.spine.testing.compiler.excluding
import io.spine.tools.compiler.ast.File
import io.spine.tools.compiler.ast.matches
import io.spine.tools.compiler.protobuf.file
import io.spine.tools.compiler.protobuf.toMessageType
import io.spine.tools.compiler.render.TypeListActions
import java.nio.file.Path
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`SignalDiscovery` should")
internal class SignalDiscoverySpec {

    companion object : SignalPluginTestSetup()

    @Test
    fun `discover all signals`(@TempDir projectDir: Path) {
        val signalSettings = createSettings(projectDir)
        // Keep compile-fail fixtures (see `defaultExclusions`) out of discovery,
        // as they intentionally fail compilation when processed.
        val setup = setup(projectDir, signalSettings, excluding(defaultExclusions()))
        val pipeline = setup.createPipeline()

        // Compile-fail fixtures are excluded from the pipeline (see `defaultExclusions`),
        // so they must not be expected among the discovered signals either.
        val excludedNames = defaultExclusions().map { it.fullName }.toSet()

        // Create a file set with the descriptors for the proto files.
        val fileSet = FileSet.of(pipeline.request.sourceFileDescriptorsList)

        // Query the states while the pipeline runs because it closes the context at the end.
        pipeline { context ->

            /**
             * Asserts that the view state specified by [viewStateClass] contains all
             * the message types declared in this proto file.
             *
             * @param fileType The type of the file such as `COMMANDS`, `EVENTS`, or `REJECTIONS`.
             * @param viewStateClass The class of an entity state such as [CommandActions],
             *   [EventActions], or [RejectionActions].
             */
            fun FileDescriptor.assertIfMatches(
                fileType: MessageFile,
                viewStateClass: Class<out EntityState<File>>,
            ) {
                val file = file()
                if (!fileType.pattern().matches(file)) {
                    return
                }
                val allMessages = messageTypes
                    .filterNot { it.fullName in excludedNames }
                    .map { it.toMessageType() }
                val state = context.select(viewStateClass).findById(file)
                state.shouldNotBeNull()
                (state as TypeListActions).getTypeList() shouldContainExactlyInAnyOrder allMessages
            }

            fileSet.files().forEach {
                it.assertIfMatches(COMMANDS, CommandActions::class.java)
                it.assertIfMatches(EVENTS, EventActions::class.java)
                it.assertIfMatches(REJECTIONS, RejectionActions::class.java)
            }
        }
    }
}
