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

package io.spine.tools.mc.java.annotation

import com.google.protobuf.Descriptors
import io.kotest.matchers.shouldBe
import io.spine.annotation.Internal
import io.spine.annotation.SPI
import io.spine.code.proto.FileDescriptors
import io.spine.code.proto.FileName
import io.spine.code.proto.FileSet
import io.spine.testing.SlowTest
import io.spine.tools.div
import io.spine.tools.fs.DirectoryName
import io.spine.tools.gradle.task.BaseTaskName
import io.spine.tools.gradle.testing.GradleProject
import io.spine.tools.gradle.testing.get
import io.spine.tools.java.fs.SourceFile
import io.spine.tools.mc.java.annotation.ApiAnnotationsPluginIgTest.Companion.moduleDir
import io.spine.tools.mc.java.annotation.check.FieldAnnotationCheck
import io.spine.tools.mc.java.annotation.check.NestedTypeFieldsAnnotationCheck
import io.spine.tools.mc.java.annotation.check.NestedTypesAnnotationCheck
import io.spine.tools.mc.java.annotation.check.SourceCheck
import io.spine.tools.mc.java.annotation.check.TypeAnnotationCheck
import io.spine.tools.mc.java.annotation.given.GivenProtoFile
import io.spine.tools.mc.java.gradle.McJavaTaskName
import java.io.File
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.name
import org.gradle.api.tasks.SourceSet
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.internal.DefaultGradleRunner
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.impl.AbstractJavaSource
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Tests annotating generated Java code with API level annotations, such as
 * [Internal] or [SPI].
 *
 * The subject of test is [io.spine.tools.mc.annotation.ApiAnnotationsPlugin] which is
 * a plugin to ProtoData. We test the plugin as a part of the Gradle build performed by
 * McJava Gradle plugin.
 *
 * The test project is located in `resources/annotator-plugin-test` directory.
 *
 * ### Enabling remote debugging for ProtoData CLI
 * Since ProtoData launches the code generation in a separate JVM, it is not possible to debug
 * it directly in an IDE. To debug the code generation, perform the following steps:
 *  1. Open the resource file `resources/annotator-plugin-test/build.gradle.kts`.
 *  2. Find the `debugOptions` block.
 *  3. Set the value of the `enabled` property to `true`. Pay attention to the fact that when
 *     enabled, the ProtoData CLI process will be suspended until a debugger is attached.
 *
 * ### Creating a remote debugging configuration in IDEA
 *  Check or create `LaunchProtoData Remote Debug` Run/Debug configuration is available in IDEA:
 *  1. Open the Run/Debug configuration window.
 *  2. Under "Remote JVM Debug" on the left, check if the `LaunchProtoData Remote Debug` is
 *     present, if not, create it via the "+" button at the top left of the dialog.
 *  3. Under "Debugger mode" select "Attach to remote JVM".
 *  4. Under "Host" enter `localhost`.
 *  5. Under "Port" enter `5566`.
 *  6. Under "Use module classpath" selection `io.spine.tools.mc-java-annotations.test`.
 *
 *  ### Starting debugging session
 *  1. Put a breakpoint on `project.executeTask(launchProtoData)` in the `AnnotatorPluginSpec`.
 *  2. Run "Debug 'AnnotatorPluginSpec'" configuration.
 *  3. Once the breakpoint is reached, hit F8 or F9 to run the `launchProtoData` task.
 *     It would take several seconds, depending on the performance of your workstation.
 *     The task will be suspended until the debugger is attached.
 *  4. Once the task is suspended, put a breakpoint in the place of interest of your
 *     [Plugin] or [Renderer] code, which is called by ProtoData.
 *  5. Run the "LaunchProtoData Remote Debug" configuration. You should see a console message
 *     about attaching to a process. If attaching to a process fails, it could mean that
 *     ProtoData CLI has not been started yet. Repeat the attempt in a few seconds.
 */
@DisplayName("`ApiAnnotationsPlugin` should")
internal class ApiAnnotationsPluginIgTest {

    companion object {
        private const val RESOURCE_DIR = "annotator-plugin-test"
        private const val RESOURCE_SUB_DIR = "typedefs"

        lateinit var moduleDir: Path
        lateinit var project: GradleProject

        @BeforeAll
        @JvmStatic
        fun compileProject(@TempDir projectDir: File) {
            project = GradleProject.setupAt(projectDir)
                .fromResources(RESOURCE_DIR)
                .copyBuildSrc()
                .withSharedTestKitDirectory()
                /* Uncomment the following line to be able to debug the build.
                   Remember to turn off so that tests run faster, AND Windows build does not
                   fail with the error on Windows Registry unavailability. */
                //.enableRunnerDebug()
                .create()
            (project.runner as DefaultGradleRunner).withJvmArguments(
                "-Xmx8g",
                "-XX:MaxMetaspaceSize=1512m",
                "-XX:+UseParallelGC",
                "-XX:+HeapDumpOnOutOfMemoryError"
            )
            moduleDir = projectDir.toPath() / RESOURCE_SUB_DIR
            project.executeTask(McJavaTaskName.launchProtoData)
        }
    }

    @Nested
    inner class
    `annotate with '@Internal' when '(internal_all) = true'` {

        @Nested
        inner class
        `and 'java_multiple_files = false'` {

            @Test
            fun `an outer class of generated messages`() =
                checkOuterClassAnnotations(
                    GivenProtoFile.OUTER_INTERNAL,
                    Internal::class.java,
                    true
                )
        }

        @Nested
        inner class
        `and 'java_multiple_files = true'` {

            @Test
            fun `top level message classes`() =
                checkTypeAnnotations(
                    GivenProtoFile.INTERNAL_ALL_MULTIPLE,
                    Internal::class.java,
                    true
                )
        }
    }

    @Nested
    inner class
    `annotate with '@Internal'` {

        @Nested
        inner class
        `when 'java_multiple_files = false'` {

            /**
             * In this test, we check that all the types nested in the outer class are annotated.
             * This is a shortcut, which is possible because all the message files in this
             * proto file are annotated with `(internal_type) = true`.
             */
            @Test
            fun `a nested class of a message type marked '(internal_type) = true`() =
                checkNestedTypesAnnotations(
                    GivenProtoFile.INTERNAL_MESSAGE,
                    Internal::class.java,
                    true
                )

            @Test
            fun `accessors for fields with '(internal) = true'`() =
                checkFieldAnnotations(GivenProtoFile.INTERNAL_FIELD, Internal::class.java, true)
        }

        @Nested
        inner class
        `when 'java_multiple_files = true'` {

            @Test
            fun `accessors for fields with '(internal) = true'`() =
                checkFieldAnnotationsMultiple(
                    GivenProtoFile.INTERNAL_FIELD_MULTIPLE,
                    Internal::class.java,
                    true
                )
        }
    }

    @Nested
    inner class
    `annotate with '@SPI'` {

        @Test
        fun `gRPC services if service option is true`() =
            checkServiceAnnotations(GivenProtoFile.SPI_SERVICE, SPI::class.java, true)

        @Test
        fun `gRPC services if 'SPI_all = true'`() =
            checkServiceAnnotations(GivenProtoFile.SPI_ALL, SPI::class.java, true)

        @Test
        fun `message class when 'SPI_all = true'`() =
            checkTypeAnnotations(GivenProtoFile.SPI_ALL, Internal::class.java, true)
    }

    @Nested
    internal inner class
    `not annotate` {

        @Test
        fun `if file option if false`() =
            checkNestedTypesAnnotations(
                GivenProtoFile.NO_INTERNAL_OPTIONS,
                Internal::class.java,
                false
            )

        @Test
        fun `service if file option is false`() =
            checkNestedTypesAnnotations(
                GivenProtoFile.NO_INTERNAL_OPTIONS,
                Internal::class.java,
                false
            )

        @Test
        fun `multiple files if file option is false`() =
            checkTypeAnnotations(
                GivenProtoFile.NO_INTERNAL_OPTIONS_MULTIPLE,
                Internal::class.java,
                false
            )

        @Test
        fun `if message option is false`() =
            checkNestedTypesAnnotations(
                GivenProtoFile.NO_INTERNAL_OPTIONS,
                Internal::class.java,
                false
            )

        @Test
        fun `multiple files if message option is false`() =
            checkTypeAnnotations(
                GivenProtoFile.NO_INTERNAL_OPTIONS_MULTIPLE,
                Internal::class.java,
                false
            )

        @Test
        fun `accessors if field option is false`() =
            checkFieldAnnotations(GivenProtoFile.NO_INTERNAL_OPTIONS, Internal::class.java, false)

        @Test
        fun `accessors in multiple files if field option is false`() =
            checkFieldAnnotationsMultiple(
                GivenProtoFile.NO_INTERNAL_OPTIONS_MULTIPLE,
                Internal::class.java,
                false
            )

        @Test
        fun `gRPC services if service option is false`() =
            checkServiceAnnotations(GivenProtoFile.NO_INTERNAL_OPTIONS, Internal::class.java, false)

        @Test
        fun `if message option overrides file option`() =
            checkNestedTypesAnnotations(GivenProtoFile.REVERTING, Internal::class.java, false)
    }

    @Test
    @SlowTest
    fun `produce Java source that compiles`() {
        val result = project.executeTask(BaseTaskName.build)
        result[BaseTaskName.build] shouldBe TaskOutcome.SUCCESS
    }
}

private val Path.generatedMain: Path
    get() = this / "generated/main"

private fun checkServiceAnnotations(
    testFile: GivenProtoFile,
    expectedAnnotation: Class<out Annotation>,
    shouldBeAnnotated: Boolean
) {
    val fileDescriptor = descriptorOf(testFile.fileName())
    val services = fileDescriptor.services
    for (serviceDescriptor in services) {
        val serviceFile = SourceFile.forService(serviceDescriptor)
        val check = TypeAnnotationCheck(
            expectedAnnotation,
            shouldBeAnnotated
        )
        check.verifyService(serviceFile)
    }
}

private fun checkFieldAnnotations(
    testFile: GivenProtoFile,
    expectedAnnotation: Class<out Annotation>,
    shouldBeAnnotated: Boolean
) {
    val fileDescriptor = descriptorOf(testFile.fileName())
    val messageDescriptor = fileDescriptor.messageTypes[0]
    val sourcePath = SourceFile.forMessage(messageDescriptor).path()

    NestedTypeFieldsAnnotationCheck(messageDescriptor, expectedAnnotation, shouldBeAnnotated)
        .verify(sourcePath)
}

private fun checkFieldAnnotationsMultiple(
    testFile: GivenProtoFile,
    expectedAnnotation: Class<out Annotation>,
    shouldBeAnnotated: Boolean
) {
    val fileDescriptor = descriptorOf(testFile.fileName())
    val messageDescriptor = fileDescriptor.messageTypes[0]
    val experimentalField = messageDescriptor.fields[0]
    val sourcePath = SourceFile.forMessage(messageDescriptor).path()

    FieldAnnotationCheck(experimentalField, expectedAnnotation, shouldBeAnnotated)
        .verify(sourcePath)
}

private fun checkTypeAnnotations(
    testFile: GivenProtoFile,
    expectedAnnotation: Class<out Annotation>,
    shouldBeAnnotated: Boolean
) {
    val fileDescriptor = descriptorOf(testFile.fileName())
    for (messageDescriptor in fileDescriptor.messageTypes) {
        val messageProto = messageDescriptor.toProto()
        val fileProto = fileDescriptor.toProto()
        val messagePath = SourceFile.forMessage(messageProto, fileProto).path()

        TypeAnnotationCheck(
            expectedAnnotation,
            shouldBeAnnotated
        ).verify(messagePath)
    }
}

private fun checkNestedTypesAnnotations(
    testFile: GivenProtoFile,
    expectedAnnotation: Class<out Annotation>,
    shouldBeAnnotated: Boolean
) {
    val fileDescriptor = descriptorOf(testFile.fileName())
    val sourcePath = SourceFile.forOuterClassOf(fileDescriptor.toProto()).path()

    NestedTypesAnnotationCheck(expectedAnnotation, shouldBeAnnotated).verify(sourcePath)
}

private fun checkOuterClassAnnotations(
    testFile: GivenProtoFile,
    expectedAnnotation: Class<out Annotation>,
    shouldBeAnnotated: Boolean
) {
    val fileDescriptor = descriptorOf(testFile.fileName())
    val sourcePath = SourceFile.forOuterClassOf(fileDescriptor.toProto()).path()

    TypeAnnotationCheck(expectedAnnotation, shouldBeAnnotated).verify(sourcePath)
}

@Suppress("UNCHECKED_CAST")
private fun parse(file: Path): AbstractJavaSource<JavaClassSource> {
    val javaSource = Roaster.parse(AbstractJavaSource::class.java, file.toFile())
    return javaSource as AbstractJavaSource<JavaClassSource>
}

private fun SourceCheck.verify(sourcePath: Path) {
    val filePath = moduleDir.generatedMain / DirectoryName.java / sourcePath
    val javaSource = parse(filePath)
    accept(javaSource)
}

private fun SourceCheck.verifyService(serviceFile: SourceFile) {
    val filePath = moduleDir.generatedMain / DirectoryName.grpc / serviceFile.path()
    val javaSource = parse(filePath)
    accept(javaSource)
}

private fun descriptorOf(testFile: FileName): Descriptors.FileDescriptor {
    val mainDescriptor = mainDescriptorPath()
    val fileSet = FileSet.parse(mainDescriptor.toFile())
    val file = fileSet.tryFind(testFile)
    check(file.isPresent) { "Unable to get file descriptor for `$testFile`." }
    return file.get()
}

/**
 * Composes the path to the main descriptor set file using the project Maven coordinates
 * as defined in the test project under `resources/annotator-plugin-test`.
 */
private fun mainDescriptorPath(): Path =
    moduleDir / DirectoryName.build / DirectoryName.descriptors / SourceSet.MAIN_SOURCE_SET_NAME /
            "io.spine.test_${moduleDir.name}_3.14${FileDescriptors.DESC_EXTENSION}"
