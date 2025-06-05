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

package io.spine.tools.mc.java.gradle.plugins

import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldNotBe
import io.spine.protodata.gradle.CodegenSettings
import io.spine.protodata.gradle.plugin.Extension
import io.spine.protodata.gradle.plugin.LaunchProtoData
import io.spine.tools.gradle.testing.GradleProject
import io.spine.tools.mc.annotation.ApiAnnotationsPlugin
import io.spine.tools.mc.java.gradle.GradleProjects.evaluate
import io.spine.tools.mc.java.gradle.given.StubProject
import io.spine.tools.mc.java.gradle.plugins.ProtoDataConfigPlugin.Companion.VALIDATION_PLUGIN_CLASS
import io.spine.tools.mc.java.signal.rejection.RThrowablePlugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`ProtoDataConfigPlugin` should")
internal class ProtoDataConfigPluginSpec {

    companion object {

        lateinit var project: Project
        lateinit var codegenSettings: Extension

        @BeforeAll
        @JvmStatic
        fun createProjectAndPlugins() {
            project = StubProject.createFor(this::class.java)
                .withMavenRepositories()
                .get()

            val plugins = project.pluginManager
            plugins.apply(GradleProject.javaPlugin)
            plugins.apply("com.google.protobuf")
            plugins.apply(McJavaPlugin::class.java)

            evaluate(project)

            codegenSettings = project.extensions.findByType<CodegenSettings>() as Extension
        }
    }

    @Test
    fun `add project extension`() {
        codegenSettings shouldNotBe null
    }

    @Test
    fun `add ProtoData plugins`() {
        val plugins = codegenSettings.plugins.get()
        plugins.shouldContainInOrder(
            VALIDATION_PLUGIN_CLASS,
            RThrowablePlugin::class.java.name,
            ApiAnnotationsPlugin::class.java.name
        )
    }

    @Test
    fun `add a task for passing configuration file`() {
        val task = project.tasks.withType<WriteProtoDataSettings>()
        task shouldNotBe null
        task.shouldNotBeEmpty()
    }

    @Test
    fun `add a task for launching ProtoData CLI`() {
        val task = project.tasks.withType<LaunchProtoData>()
        task shouldNotBe null
        task.shouldNotBeEmpty()
    }
}
