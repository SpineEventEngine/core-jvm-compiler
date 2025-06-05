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

package io.spine.tools.mc.java.ksp.gradle

import com.google.devtools.ksp.gradle.KspAATask
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.spine.io.toUnix
import io.spine.testing.SlowTest
import io.spine.tools.gradle.project.sourceSets
import io.spine.tools.mc.java.ksp.gradle.KspBasedPluginTest.Companion.removeTempDir
import java.io.File
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.problems.Problem
import org.gradle.api.problems.ProblemId
import org.gradle.api.problems.ProblemReporter
import org.gradle.api.problems.ProblemSpec
import org.gradle.api.problems.internal.InternalProblem
import org.gradle.api.problems.internal.InternalProblemBuilder
import org.gradle.api.problems.internal.InternalProblemReporter
import org.gradle.api.problems.internal.InternalProblemSpec
import org.gradle.api.problems.internal.InternalProblems
import org.gradle.api.problems.internal.ProblemsInfrastructure
import org.gradle.api.problems.internal.ProblemsProgressEventEmitterHolder
import org.gradle.internal.operations.OperationIdentifier
import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.CleanupMode
import org.junit.jupiter.api.io.TempDir

@DisplayName("`KspBasedPlugin` should")
@SlowTest
internal class KspBasedPluginTest {

    companion object {

        private lateinit var projectDir: File
        private lateinit var project: Project

        /**
         * Creates the project in the given directory.
         *
         * The directory is set not to be cleaned up by JUnit because cleanup sometimes
         * fails under Windows.
         * See [this comment](https://github.com/gradle/gradle/issues/12535#issuecomment-1064926489)
         * on the corresponding issue for details:
         *
         * The [projectDir] is set to be removed in the [removeTempDir] method.
         *
         * @see removeTempDir
         */
        @BeforeAll
        @JvmStatic
        fun setupProject(@TempDir(cleanup = CleanupMode.NEVER) projectDir: File) {
            this.projectDir = projectDir
            
            // See: https://github.com/gradle/gradle/issues/31862#issuecomment-2687633265
            // and stub classes below.
            ProblemsProgressEventEmitterHolder.init(InternalProblemsStub())

            project = ProjectBuilder.builder()
                .withProjectDir(projectDir)
                .build()

            project.buildscript.repositories.run {
                mavenLocal()
                mavenCentral()
            }

            project.pluginManager.run {
                apply("java")
                // This plugin is in the test classpath dependency, so it comes without the version.
                apply("org.jetbrains.kotlin.jvm")
                apply(StubPlugin::class.java)
            }

            val customSourceSet = "integrationTest"
            project.sourceSets.run {
                create(customSourceSet)
            }

            val kotlinFile = projectDir.resolve("src/${customSourceSet}/kotlin/Test.kt")
            kotlinFile.parentFile.mkdirs()
            kotlinFile.writeText("class Test")
            kotlinFile.exists() shouldBe true

            // Force evaluation of the project.
            project.evaluationDependsOn(":")
        }

        @AfterAll
        @JvmStatic
        fun removeTempDir() {
            projectDir.deleteOnExit()
        }
    }

    @Test
    fun `apply the KSP plugin`() {
        project.pluginManager.hasPlugin(KspGradlePlugin.id) shouldNotBe null
    }

    @Test
    @Disabled("Until `File.toRelativeString()` in `kotlin.io.Utils.kt` supports our case")
    fun `redirect KSP tasks output`() {
        val projectRoot = project.projectDir.absolutePath.toUnix()
        val tasks = project.tasks.withType<KspAATask>()
        tasks.size shouldBeGreaterThan 0
        tasks.forEach { task ->
            arrayOf(
                task.kspConfig.outputBaseDir,
                task.kspConfig.javaOutputDir,
                task.kspConfig.kotlinOutputDir
            ).forEach {
                it.get().absolutePath.toUnix().let { path ->
                    path shouldNotContain "/build/"
                    path shouldContain "$projectRoot/generated/ksp"
                }
            }
        }
    }
}

private class StubPlugin : KspBasedPlugin() {

    /**
     * We don't need real coordinates in this property because no compilation
     * takes place in these tests.
     */
    override val mavenCoordinates: String = "org.example:core:1.0.0"
}

/**
 * The stub class for workaround for
 * [this Gradle issue](https://github.com/gradle/gradle/issues/31862).
 *
 * @see <a href="https://github.com/gradle/gradle/issues/31862#issuecomment-2687633265">
 *     Workaround</a>
 */
private class InternalProblemsStub : InternalProblems {
    override fun getReporter(): ProblemReporter = notImplemented()
    override fun getInternalReporter(): InternalProblemReporter = InternalProblemReporterStub()
    override fun getInfrastructure(): ProblemsInfrastructure = notImplemented()
    override fun getProblemBuilder(): InternalProblemBuilder = notImplemented()
}

private fun notImplemented(): Nothing = TODO("Not yet implemented")

/**
 * The stub class for workaround for
 * [this Gradle issue](https://github.com/gradle/gradle/issues/31862).
 *
 * @see <a href="https://github.com/gradle/gradle/issues/31862#issuecomment-2687633265">
 *     Workaround</a>
 */
private class InternalProblemReporterStub : InternalProblemReporter {
    override fun create(problemId: ProblemId, action: Action<in ProblemSpec>): Problem =
        notImplemented()
    override fun report(problem: Problem, id: OperationIdentifier) = notImplemented()
    override fun report(problemId: ProblemId, spec: Action<in ProblemSpec>) = notImplemented()
    override fun report(problem: Problem) = notImplemented()
    override fun report(problems: MutableCollection<out Problem>) = notImplemented()

    override fun throwing(
        exception: Throwable,
        problemId: ProblemId,
        spec: Action<in ProblemSpec>
    ): RuntimeException = notImplemented()

    override fun throwing(exception: Throwable, problem: Problem): RuntimeException =
        notImplemented()

    override fun throwing(
        exception: Throwable,
        problems: MutableCollection<out Problem>
    ): RuntimeException = notImplemented()

    override fun internalCreate(action: Action<in InternalProblemSpec>): InternalProblem =
        notImplemented()
}
