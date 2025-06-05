/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.spine.code.java.SimpleClassName
import io.spine.testing.SlowTest
import io.spine.testing.TempDir
import io.spine.tools.gradle.testing.GradleProject.Companion.setupAt
import io.spine.tools.java.fullTextNormalized
import io.spine.tools.mc.java.gradle.McJavaTaskName.Companion.launchProtoData
import io.spine.tools.mc.java.signal.rejection.Javadoc.BUILD_METHOD_ABSTRACT
import io.spine.tools.mc.java.signal.rejection.Javadoc.NEW_BUILDER_METHOD_ABSTRACT
import io.spine.tools.mc.java.signal.rejection.JavadocTestEnv.expectedBuilderClassComment
import io.spine.tools.mc.java.signal.rejection.JavadocTestEnv.expectedClassComment
import io.spine.tools.mc.java.signal.rejection.JavadocTestEnv.expectedFirstFieldComment
import io.spine.tools.mc.java.signal.rejection.JavadocTestEnv.expectedSecondFieldComment
import io.spine.tools.mc.java.signal.rejection.JavadocTestEnv.rejectionFileContent
import io.spine.tools.mc.java.signal.rejection.JavadocTestEnv.rejectionJavaFile
import io.spine.tools.mc.java.signal.rejection.Method.BUILD
import io.spine.tools.mc.java.signal.rejection.Method.NEW_BUILDER
import org.gradle.testkit.runner.internal.DefaultGradleRunner
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.JavaDocCapableSource
import org.jboss.forge.roaster.model.source.MethodSource
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@SlowTest
@DisplayName("Rejection code generator should produce Javadoc for")
internal class RejectionJavadocIgTest {

    companion object {

        private lateinit var generatedSource: JavaClassSource
        private lateinit var builderType: JavaClassSource

        @BeforeAll
        @JvmStatic
        fun generateSources() {
            val projectDir = TempDir.forClass(RejectionJavadocIgTest::class.java)
            val project = setupAt(projectDir)
                .copyBuildSrc()
                .withSharedTestKitDirectory()
                .fromResources("rejection-javadoc-test") // Provides `build.gradle.kts`
                .addFile(
                    "sub-module/src/main/proto/javadoc_rejections.proto",
                    rejectionFileContent()
                )
                .create()
            (project.runner as DefaultGradleRunner).withJvmArguments(
                "-Xmx8g",
                "-XX:MaxMetaspaceSize=1512m",
                "-XX:+UseParallelGC",
                "-XX:+HeapDumpOnOutOfMemoryError"
            )
            project.executeTask(launchProtoData)
            val generatedFile = rejectionJavaFile(projectDir.resolve("sub-module"))
            generatedSource = Roaster.parse(
                JavaClassSource::class.java, generatedFile
            )
            val builderTypeName = SimpleClassName.ofBuilder().value
            builderType = generatedSource.getNestedType(builderTypeName) as JavaClassSource
        }
    }

    @Test
    fun `'RejectionThrowable' class`() {
        assertDoc(expectedClassComment(), generatedSource)
    }

    @Test
    fun `'newBuilder' method`() {
        assertMethodDoc(NEW_BUILDER_METHOD_ABSTRACT, generatedSource, NEW_BUILDER)
    }

    @Test
    fun `'Builder' class under the 'RejectionThrowable'`() {
        assertDoc(expectedBuilderClassComment(), builderType)
    }

    @Test
    fun `'build()' method of the 'Builder' class`() {
        assertMethodDoc(BUILD_METHOD_ABSTRACT, builderType, BUILD)
    }

    @Test
    fun `property setters of the builder`() {
        assertMethodDoc(expectedFirstFieldComment(), builderType, "setId")
        assertMethodDoc(expectedSecondFieldComment(), builderType, "setRejectionMessage")
    }
}

private fun assertDoc(expectedText: String, source: JavaDocCapableSource<*>) {
    val javadoc = source.javaDoc
    javadoc.fullTextNormalized() shouldBe expectedText
}

private fun assertMethodDoc(
    expectedComment: String,
    source: JavaClassSource,
    methodName: String
) {
    val method = source.findMethod(methodName)
    assertDoc(expectedComment, method)
}

private fun JavaClassSource.findMethod(methodName: String): MethodSource<JavaClassSource> =
    methods.stream()
        .filter { methodName == it.name }
        .findFirst()
        .orElseThrow { error("Cannot find the method `$methodName`.") }
