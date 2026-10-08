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
