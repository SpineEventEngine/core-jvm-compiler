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

package io.spine.tools.core.jvm.entity

import io.kotest.matchers.string.shouldContain
import io.spine.base.AggregateState
import io.spine.base.EntityState
import io.spine.base.ProcessManagerState
import io.spine.base.ProjectionState
import io.spine.tools.code.Java
import io.spine.tools.compiler.render.SourceFile
import io.spine.tools.core.jvm.entity.EntityPluginTestSetup.Companion.java
import io.spine.tools.java.reference
import java.nio.file.Path
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`ImplementEntityState` action should")
class ImplementEntityStateSpec {

    private lateinit var sourceFile: SourceFile<Java>

    companion object : EntityPluginTestSetup() {

        @BeforeAll
        @JvmStatic
        fun setup(@TempDir projectDir: Path) {
            runPipeline(projectDir)
        }
    }

    @Test
    fun `use 'AggregateState' interface for 'AGGREGATE' kind`() {
        sourceFile = file("Employee".java)
        sourceFile.code().shouldContain(AggregateState::class.java.reference)
    }

    @Test
    fun `use 'ProjectionState' interface for 'PROJECTION' kind`() {
        sourceFile = file("Organization".java)
        sourceFile.code().shouldContain(ProjectionState::class.java.reference)
    }

    @Test
    fun `use 'ProcessManagerState' interface for 'PROCESS_MANAGER' kind`() {
        sourceFile = file("Transition".java)
        sourceFile.code().shouldContain(ProcessManagerState::class.java.reference)
    }

    @Test
    fun `use 'EntityState' interface for 'ENTITY' kind`() {
        sourceFile = file("Blob".java)
        sourceFile.code().shouldContain(EntityState::class.java.reference)
    }
}
