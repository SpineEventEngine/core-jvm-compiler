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

package io.spine.tools.core.jvm.gradle.plugins

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.spine.tools.core.jvm.gradle.CoreJvmOptions
import io.spine.tools.core.jvm.gradle.GradleProjects.evaluate
import io.spine.tools.core.jvm.gradle.given.StubProject
import io.spine.tools.gradle.testing.GradleProject
import io.spine.type.toJson
import java.io.File
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`WriteCompilerPluginsSettings` should")
internal class WriteCompilerPluginsSettingsSpec {

    companion object {

        /**
         * The extension of the settings files in the Protobuf JSON format.
         */
        private const val JSON = "pb.json"

        lateinit var project: Project

        @BeforeAll
        @JvmStatic
        fun createProject(@TempDir tempDir: File) {
            project = StubProject.createIn(tempDir)
                .withMavenRepositories()
                .get()

            val plugins = project.pluginManager
            plugins.apply(GradleProject.javaPlugin)
            plugins.apply("com.google.protobuf")
            plugins.apply(CoreJvmPlugin::class.java)

            evaluate(project)
        }
    }

    @Test
    fun `write the settings of each Compiler plugin in JSON`() {
        val task = project.tasks.withType<WriteCompilerPluginsSettings>().single()
        val settings = task.settings.get()

        task.writeFiles()

        val dir = task.settingsDir.get().asFile
        dir.list().orEmpty().toList() shouldContainExactlyInAnyOrder
                settings.keys.map { "$it.$JSON" }
        settings.forEach { (id, message) ->
            dir.resolve("$id.$JSON").readText() shouldBe message.toJson()
        }
    }

    @Test
    fun `require the compiler options to obtain the settings`() {
        val options = project.objects.newInstance(CoreJvmOptions::class.java)
        shouldThrow<IllegalStateException> {
            options.compilerPluginSettings()
        }
    }
}
