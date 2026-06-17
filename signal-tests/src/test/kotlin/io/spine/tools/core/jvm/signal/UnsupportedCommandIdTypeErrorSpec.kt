/*
 * Copyright 2026, TeamDev. All rights reserved.
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

package io.spine.tools.core.jvm.signal

import io.kotest.matchers.string.shouldContain
import io.spine.testing.compiler.acceptingOnly
import io.spine.tools.core.jvm.assertCompilationError
import io.spine.tools.core.signal.given.command.RepeatedIdCommand
import java.nio.file.Path
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Tests that a command with a target-entity ID field of an unsupported type fails
 * compilation.
 *
 * The exhaustive matrix of supported and unsupported ID types is covered by
 * `SupportedIdTypeSpec` in the `base` module. This suite checks that such a
 * rejection surfaces end-to-end through the signal code generation pipeline.
 *
 * It lives in its own class because each suite runs the pipeline only once
 * (see the `Empty`-ID case in `CommandIdErrorSpec`).
 */
@DisplayName("`CommandTargetReaction` should")
internal class UnsupportedCommandIdTypeErrorSpec {

    companion object : SignalPluginTestSetup()

    @Test
    fun `reject the target-entity ID field of a 'repeated' type`(@TempDir projectDir: Path) {
        val descriptor = RepeatedIdCommand.getDescriptor()
        val error = assertCompilationError {
            runPipeline(projectDir, acceptingOnly(descriptor))
        }
        error.message.let {
            it shouldContain "${descriptor.fullName}.telescope"
            it shouldContain "repeated string"
            it shouldContain "is not supported"
        }
    }
}
