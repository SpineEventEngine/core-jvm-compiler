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

package io.spine.tools.mc.java.signal.rejection

import io.kotest.matchers.shouldBe
import io.spine.testing.SlowTest
import io.spine.tools.code.SourceSetName
import io.spine.tools.code.SourceSetName.Companion.main
import io.spine.tools.code.SourceSetName.Companion.test
import io.spine.tools.fs.DirectoryName.generated
import io.spine.tools.fs.DirectoryName.java
import io.spine.tools.gradle.task.JavaTaskName.Companion.compileTestJava
import io.spine.tools.gradle.testing.GradleProject
import io.spine.tools.resolve
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import org.gradle.testkit.runner.internal.DefaultGradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Tests the code generation of rejections.
 *
 * The test project is located in `rejection-codegen-test` directory in the test resources.
 * Static initialization of the test class copies the project to a temporary directory and
 * configures Gradle to run the code generation task by executing the [compileTestJava] task,
 * which depends on code generation tasks.
 *
 * Running Java compilation ensures that:
 *  1. The code generation is executed for both `main` and `test` source sets.
 *  2. The generated code is compiled.
 *
 * Test methods of this test suite check the presence of the generated code in
 * the expected locations.
 *
 * The content of the generated code is partially tested in [RejectionJavadocIgTest] suite
 * dedicated to checking the correctness of Javadoc comments.
 *
 * @see RejectionJavadocIgTest
 */
@SlowTest
@DisplayName("Code generation of rejections should")
internal class RejectionCodegenIgTest {

    companion object {

        /**
         * The directory where the generated code is expected to be located.
         */
        private const val PACKAGE_DIR = "io/spine/sample/rejections"
        
        private lateinit var moduleDir: File

        @TempDir
        lateinit var projectDir: File

        @BeforeAll
        @JvmStatic
        fun generateRejections() {
            val project: GradleProject = GradleProject.setupAt(projectDir)
                .fromResources("rejection-codegen-test")
                .copyBuildSrc()
                .withSharedTestKitDirectory()
                /*
                   Running tests with `enableRunnerDebug()` turned on
                   ---------------------------------------------------
                   Uncomment the following line to be able to debug the Gradle build process.
                   Remember to turn it OFF before committing your code so that tests run faster.

                   IMPORTANT: Running with `enableRunnerDebug()` turned on fails
                   under Windows in the CI environment because internally Gradle tries to
                   access the Windows Registry, which requires special permissions for a process.
                */
                 //.enableRunnerDebug()
                .create()
            waitForBuildSrc(projectDir)
            (project.runner as DefaultGradleRunner).withJvmArguments(
                "-Xmx8g",
                "-XX:MaxMetaspaceSize=1512m",
                "-XX:+UseParallelGC",
                "-XX:+HeapDumpOnOutOfMemoryError"
            )
            moduleDir = projectDir.toPath()
                .resolve("sub-module")
                .toFile()

            // Executing the `compileTestJava` task should generate rejection types from both
            // `test` and `main` source sets.
            project.executeTask(compileTestJava)
        }

        /**
         * This function waits till the `buildSrc/build.gradle.kts` is fully
         * available to avoid the issues during parallel Gradle task execution.
         */
        @Suppress("LoopWithTooManyJumpStatements")
        private fun waitForBuildSrc(projectDir: File) {
            val buildGradleFile = projectDir.resolve("buildSrc/build.gradle.kts")
            fun wait() = Thread.sleep(100)
            while (!buildGradleFile.exists()) {
                // Wait for the build script to appear.
                // The tests fail to see the file during parallel execution of Gradle.
                wait()
            }
            var fileReady = false
            while (fileReady.not()) {
                try {
                    // See if the file is fully written by expecting the final closing brace.
                    val lines = buildGradleFile.readLines()
                    if (!lines.any { it.startsWith("}")}) {
                        wait()
                        continue
                    }
                } catch (_: Exception) {
                    // The file is not ready yet.
                    wait()
                    continue
                }
                fileReady = true
            }
        }
    }

    private fun generatedRoot(sourceSetName: SourceSetName): Path =
        moduleDir.toPath().resolve(generated).resolve(sourceSetName.value)

    private fun targetMainDir(): Path = generatedRoot(main)

    private fun targetTestDir(): Path = generatedRoot(test)

    @Nested internal inner class
    `place generated code under the directory of` {

        @Test
        fun `'main' source set`() {
            val mainJava = targetMainDir().resolve(java)
            assertExists(mainJava)
            mainJava.isDirectory() shouldBe true
            mainJava.containsJavaFiles() shouldBe true
        }

        @Test
        fun `'test' source set`() {
            val testJava = targetTestDir().resolve(java)
            assertExists(testJava)
            testJava.isDirectory() shouldBe true
            testJava.containsJavaFiles() shouldBe true
        }

        @Test
        fun `'testFixtures' source set`() {
            val testFixturesSpine = generatedRoot(SourceSetName("testFixtures")).resolve(java)
            assertExists(testFixturesSpine)
            testFixturesSpine.isDirectory() shouldBe true
            testFixturesSpine.containsJavaFiles() shouldBe true
        }
    }

    @Nested internal inner class
    `use the package specified in proto file options for` {

        @Test
        fun `'main' source set`() {
            // As defined in `resources/.../main_rejections.proto`.
            val packageDir = targetMainDir().resolve(java).resolve(PACKAGE_DIR)
            assertExists(packageDir)

            // As defined in `resources/.../main_rejections.proto`.
            assertJavaFileExists(packageDir, "Rejection1")
            assertJavaFileExists(packageDir, "Rejection2")
            assertJavaFileExists(packageDir, "Rejection3")
            assertJavaFileExists(packageDir, "Rejection4")
            assertJavaFileExists(packageDir, "RejectionWithRepeatedField")
            assertJavaFileExists(packageDir, "RejectionWithMapField")
        }

        @Test
        fun `'test' source set`() {
            // As defined in `resources/.../test_rejections.proto`.
            val packageDir = targetTestDir().resolve(java).resolve(PACKAGE_DIR)
            assertExists(packageDir)

            // As defined in `resources/.../test_rejections.proto`.
            assertJavaFileExists(packageDir, "TestRejection1")
            assertJavaFileExists(packageDir, "TestRejection2")
        }
    }
}

private fun Path.containsJavaFiles(): Boolean {
    val found = toFile().walk().find { file -> file.name.endsWith(".java") }
    return found != null
}

private fun assertExists(path: Path) =
    assertTrue(Files.exists(path)) { "The path `$path` is expected to exist." }

private fun assertJavaFileExists(packageDir: Path, typeName: String) {
    val file = packageDir.resolve("$typeName.java")
    assertExists(file)
}
