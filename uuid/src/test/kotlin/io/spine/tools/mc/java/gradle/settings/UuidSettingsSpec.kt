/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.tools.mc.java.gradle.settings

import io.kotest.matchers.collections.shouldContainExactly
import io.spine.protodata.java.render.ImplementInterface
import io.spine.tools.kotlin.reference
import io.spine.tools.mc.java.uuid.AddFactoryMethods
import java.io.File
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * This is a test suite for [UuidSettings] class which belongs to `mc-java-base` module.
 *
 * We have this test suite in another module to check the correctness of default settings
 * specified as strings against classes of this module.
 */
@DisplayName("`UuidSettings` should")
internal class UuidSettingsSpec {

    @Test
    fun `provide default action class names`(@TempDir projectDir: File) {
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        val settings = UuidSettings(project)

        val expected = setOf(
            ImplementInterface::class.java.name,
            AddFactoryMethods::class.reference,
        )

        settings.actions().actionMap.keys shouldContainExactly expected
        settings.toProto().actions.actionMap.keys shouldContainExactly expected
    }
}
