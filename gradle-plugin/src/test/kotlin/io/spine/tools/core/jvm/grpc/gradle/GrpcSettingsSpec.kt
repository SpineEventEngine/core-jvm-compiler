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

package io.spine.tools.core.jvm.grpc.gradle

import com.google.protobuf.gradle.GenerateProtoTask
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.spine.tools.core.jvm.gradle.GradleProjects.evaluate
import io.spine.tools.core.jvm.gradle.given.StubProject
import io.spine.tools.gradle.testing.GradleProject
import java.io.File
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`GrpcSettings` should")
internal class GrpcSettingsSpec {

    companion object {

        private lateinit var enabled: StubGrpcProject
        private lateinit var disabled: StubGrpcProject

        @BeforeAll
        @JvmStatic
        fun createProjects(@TempDir tempDir: File) {
            enabled = StubGrpcProject(tempDir.resolve("enabled"), grpcEnabled = true)
            disabled = StubGrpcProject(tempDir.resolve("disabled"), grpcEnabled = false)
        }
    }

    @Test
    fun `not create 'GenerateProtoTask's while the project is configured`() {
        val generateProto = Regex("generate\\w*Proto")
        enabled.createdOnConfiguration.filter { it.matches(generateProto) }.shouldBeEmpty()
    }

    @Test
    fun `add gRPC plugins to 'GenerateProtoTask's created after the configuration`() {
        val tasks = enabled.generateProtoTasks()
        tasks.map { it.name } shouldContainExactlyInAnyOrder
                listOf("generateProto", "generateTestProto")
        tasks.forEach { task ->
            task.hasPlugin(JAVA_PLUGIN).shouldBeTrue()
            task.hasPlugin(KOTLIN_PLUGIN).shouldBeTrue()
        }
    }

    @Test
    fun `add the gRPC Kotlin stub library`() {
        enabled.grpcStubDependencies().shouldNotBeEmpty()
    }

    @Test
    fun `not add gRPC plugins if gRPC is not enabled`() {
        val tasks = disabled.generateProtoTasks()
        tasks.shouldNotBeEmpty()
        tasks.forEach { task ->
            task.hasPlugin(JAVA_PLUGIN).shouldBeFalse()
            task.hasPlugin(KOTLIN_PLUGIN).shouldBeFalse()
        }
        disabled.grpcStubDependencies().shouldBeEmpty()
    }
}

/**
 * The name of the `protoc` gRPC plugin for Java.
 */
private const val JAVA_PLUGIN = "grpc"

/**
 * The name of the `protoc` gRPC plugin for Kotlin.
 */
private const val KOTLIN_PLUGIN = "grpckt"

/**
 * An evaluated stub project with the Protobuf Gradle Plugin and [GrpcSettings].
 *
 * The project does not have the CoreJvm Gradle Plugin applied, because the Compiler
 * Gradle Plugin it brings creates all the `GenerateProtoTask`s while the project is configured.
 */
private class StubGrpcProject(projectDir: File, grpcEnabled: Boolean) {

    private val project: Project = StubProject.createIn(projectDir)
        .withMavenRepositories()
        .get()

    /**
     * The names of the tasks Gradle created while the project was configured.
     */
    val createdOnConfiguration: List<String>

    init {
        val created = mutableListOf<String>()
        project.tasks.configureEach { created.add(it.name) }

        val plugins = project.pluginManager
        plugins.apply(GradleProject.javaPlugin)
        plugins.apply("com.google.protobuf")
        project.extensions.create("grpc", GrpcSettings::class.java)
            .enabled.set(grpcEnabled)

        evaluate(project)
        createdOnConfiguration = created.toList()
    }

    /**
     * Obtains the `GenerateProtoTask`s of the project, making Gradle create them.
     */
    fun generateProtoTasks(): List<GenerateProtoTask> =
        project.tasks.withType<GenerateProtoTask>().toList()

    /**
     * Obtains the dependencies on the gRPC Kotlin stub library.
     */
    fun grpcStubDependencies(): List<String> =
        project.configurations.getByName("implementation").dependencies
            .filter { it.group == "io.grpc" && it.name == "grpc-kotlin-stub" }
            .map { "${it.group}:${it.name}:${it.version}" }
}
