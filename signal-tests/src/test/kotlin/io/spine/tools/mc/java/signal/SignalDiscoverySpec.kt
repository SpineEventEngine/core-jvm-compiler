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

package io.spine.tools.mc.java.signal

import com.google.protobuf.Descriptors.FileDescriptor
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.spine.base.EntityState
import io.spine.base.MessageFile
import io.spine.base.MessageFile.COMMANDS
import io.spine.base.MessageFile.EVENTS
import io.spine.base.MessageFile.REJECTIONS
import io.spine.code.proto.FileSet
import io.spine.protodata.ast.File
import io.spine.protodata.ast.matches
import io.spine.protodata.backend.Pipeline
import io.spine.protodata.protobuf.file
import io.spine.protodata.protobuf.toMessageType
import io.spine.protodata.render.TypeListActions
import io.spine.protodata.testing.PipelineSetup
import io.spine.testing.server.blackbox.BlackBox
import java.nio.file.Path
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`SignalDiscovery` should")
internal class SignalDiscoverySpec {

    companion object : SignalPluginTestSetup() {

        lateinit var setup: PipelineSetup
        lateinit var pipeline: Pipeline
        lateinit var blackbox: BlackBox

        @BeforeAll
        @JvmStatic
        fun run(@TempDir projectDir: Path) {
            val signalSettings = createSettings(projectDir)
            setup = setup(projectDir, signalSettings)
            val (p, b) = setup.createPipelineWithBlackBox()
            pipeline = p
            blackbox = b
            pipeline()
        }
    }

    @Test
    fun `discover all signals`() {
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
            val allMessages = messageTypes.map {
                it.toMessageType()
            }
            val assertEntity = blackbox.assertEntityWithState(file, viewStateClass)
            assertEntity.exists()

            val state = assertEntity.actual()!!.state() as TypeListActions
            state.getTypeList() shouldContainExactlyInAnyOrder allMessages
        }

        // Create a file set with the descriptors for the proto files.
        val fileSet = FileSet.of(pipeline.request.sourceFileDescriptorsList)

        fileSet.files().forEach {
            it.assertIfMatches(COMMANDS, CommandActions::class.java)
            it.assertIfMatches(EVENTS, EventActions::class.java)
            it.assertIfMatches(REJECTIONS, RejectionActions::class.java)
        }
    }
}
