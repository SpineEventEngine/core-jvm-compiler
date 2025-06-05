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

package io.spine.tools.mc.java

import com.google.protobuf.Descriptors.GenericDescriptor
import com.google.protobuf.Message
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.spine.protodata.backend.DescriptorFilter
import io.spine.protodata.java.style.JavaCodeStyleFormatterPlugin
import io.spine.protodata.params.Directories
import io.spine.protodata.params.WorkingDirectory
import io.spine.protodata.plugin.Plugin
import io.spine.protodata.render.SourceFile
import io.spine.protodata.render.SourceFileSet
import io.spine.protodata.settings.SettingsDirectory
import io.spine.protodata.testing.PipelineSetup
import io.spine.protodata.testing.PipelineSetup.Companion.byResources
import io.spine.protodata.testing.pipelineParams
import io.spine.protodata.testing.withRequestFile
import io.spine.protodata.testing.withSettingsDir
import io.spine.protodata.util.Format
import io.spine.tools.code.Java
import io.spine.tools.code.SourceSetName
import io.spine.tools.mc.java.gradle.settings.CodegenSettings
import io.spine.type.toJson
import java.nio.file.Path
import kotlin.io.path.exists
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

/**
 * An abstract base for companion objects of test suites testing
 * a ProtoData plugin provided by a module of McJava.
 *
 * @param S The type of the plugin settings in the form of a Protobuf message.
 */
abstract class PluginTestSetup<S: Message>(
    protected val plugin: Plugin,
    protected val settingsId: String
) {

    /**
     * The source file set generated as the result of [running a pipeline][runPipeline].
     */
    protected lateinit var sourceFileSet: SourceFileSet

    /**
     * Creates a Gradle project to be used in the tests.
     *
     * @param dir the project directory.
     */
    protected fun createProject(dir: Path): Project {
        return ProjectBuilder.builder().withProjectDir(dir.toFile()).build()
    }

    /**
     * Creates settings for the [plugin] under the test.
     *
     * Please use [createCodegenConfig] for obtaining all default code
     * generation settings and then get its part for your plugin.
     */
    protected abstract fun createSettings(projectDir: Path): S

    /**
     * Creates default code generation settings created for a project when
     * McJava Gradle plugin is applied.
     */
    protected fun createCodegenConfig(projectDir: Path): CodegenSettings {
        val project = createProject(projectDir)
        // This mimics the call `McJavaOptions` performed on `injectProject`.
        val codegenConfig = CodegenSettings(project)
        return codegenConfig
    }

    /**
     * Creates an instance of [PipelineSetup] with the given parameters.
     *
     * [settings] will be written to the [WorkingDirectory.settingsDirectory] before
     * creation of a [Pipeline][io.spine.protodata.backend.Pipeline].
     *
     * @param projectDir The directory to create the compilation environment.
     * @param settings The plugin settings used by the test.
     * @param descriptorFilter The filter to accept descriptors during the compilation.
     *   Accepts all by default.
     */
    fun setup(
        projectDir: Path,
        settings: S,
        descriptorFilter: (GenericDescriptor) -> Boolean = { true }
    ): PipelineSetup {
        val workingDir = projectDir.resolve("build").resolve(Directories.PROTODATA_WORKING_DIR)
        val workingDirectory = WorkingDirectory(workingDir)
        val requestFile = workingDirectory.requestDirectory.file(SourceSetName("testFixtures"))
        val params = pipelineParams {
            withRequestFile(requestFile)
            withSettingsDir(workingDirectory.settingsDirectory.path)
        }
        val outputDir = projectDir.resolve("output")
        outputDir.toFile().mkdirs()
        val setup = byResources(
            params = params,
            plugins = listOf(
                plugin,
                // We want to be able to see the code in debug formatted for easier reading.
                JavaCodeStyleFormatterPlugin()
            ),
            outputRoot = outputDir,
            descriptorFilter = descriptorFilter
        ) {
            writeSettings(it, settings)
        }
        return setup
    }

    private fun writeSettings(dir: SettingsDirectory, settings: S) {
        dir.write(settingsId, Format.PROTO_JSON, settings.toJson())
    }

    /**
     * Runs the pipeline with the plugin settings obtained from [createSettings].
     *
     * @param excludedDescriptors The descriptors to be excluded from the compilation.
     * @see createSettings
     */
    fun runPipeline(
        projectDir: Path,
        excludedDescriptors: List<GenericDescriptor> = listOf()
    ) {
        val descriptorFilter: DescriptorFilter = {
            excludedDescriptors.find { d -> d.fullName == it.fullName } == null
        }
        runPipeline(projectDir, descriptorFilter)
    }

    /**
     * Runs the pipeline with the plugin settings obtained from [createSettings] and
     * given descriptor filter.
     *
     * @see createSettings
     */
    fun runPipeline(
        projectDir: Path,
        descriptorFilter: DescriptorFilter
    ) {
        // Clear the cache of previously parsed files to avoid repeated code generation.
        SourceFile.clearCache()
        val settings = createSettings(projectDir)
        val setup = setup(projectDir, settings, descriptorFilter)
        val pipeline = setup.createPipeline()
        pipeline()
        this.sourceFileSet = pipeline.sources[0]
    }

    /**
     * Obtains paths to source files taking the [packageDir] and simple names of the classes.
     */
    fun files(packageDir: Path, vararg classNames: String): List<Path> {
        return classNames.map { packageDir.resolve("$it.java") }
    }

    /**
     * Locates the file with the given [path], asserting its existence.
     */
    fun file(path: Path): SourceFile<Java> {
        val file = sourceFileSet.find(path)
        file shouldNotBe null
        file!!.outputPath.exists() shouldBe true
        @Suppress("UNCHECKED_CAST")
        return file as SourceFile<Java>
    }
}
