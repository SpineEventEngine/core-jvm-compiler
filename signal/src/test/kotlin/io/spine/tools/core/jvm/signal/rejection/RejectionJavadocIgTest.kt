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

package io.spine.tools.core.jvm.signal.rejection

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaDocumentedElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.javadoc.PsiDocComment
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.spine.code.java.SimpleClassName
import io.spine.testing.SlowTest
import io.spine.tools.core.jvm.signal.rejection.Javadoc.BUILD_METHOD_ABSTRACT
import io.spine.tools.core.jvm.signal.rejection.Javadoc.NEW_BUILDER_METHOD_ABSTRACT
import io.spine.tools.core.jvm.signal.rejection.JavadocTestEnv.expectedBuilderClassComment
import io.spine.tools.core.jvm.signal.rejection.JavadocTestEnv.expectedClassComment
import io.spine.tools.core.jvm.signal.rejection.JavadocTestEnv.expectedFirstFieldComment
import io.spine.tools.core.jvm.signal.rejection.JavadocTestEnv.expectedSecondFieldComment
import io.spine.tools.core.jvm.signal.rejection.JavadocTestEnv.rejectionFileContent
import io.spine.tools.core.jvm.signal.rejection.JavadocTestEnv.rejectionJavaFile
import io.spine.tools.core.jvm.signal.rejection.Method.BUILD
import io.spine.tools.core.jvm.signal.rejection.Method.NEW_BUILDER
import io.spine.tools.gradle.task.JavaTaskName.Companion.compileJava
import io.spine.tools.gradle.testing.GradleProject.Companion.setupAt
import io.spine.tools.psi.java.Environment
import io.spine.tools.psi.java.Parser
import io.spine.tools.psi.java.topLevelClass
import java.io.File
import org.gradle.testkit.runner.internal.DefaultGradleRunner
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@SlowTest
@DisplayName("Rejection code generator should produce Javadoc for")
internal class RejectionJavadocIgTest {

    companion object {

        private lateinit var generatedClass: PsiClass
        private lateinit var builderClass: PsiClass

        @BeforeAll
        @JvmStatic
        fun generateSources(@TempDir projectDir: File) {
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
            project.executeTask(compileJava /* launchSpineCompiler */)
            val generatedFile = rejectionJavaFile(projectDir.resolve("sub-module"))
            val javaFile = Parser(Environment.project)
                .parse(generatedFile.readText(), generatedFile)
            generatedClass = javaFile.topLevelClass
            val builderClassName = SimpleClassName.ofBuilder().value
            builderClass = generatedClass.findInnerClassByName(builderClassName, false)
                ?: error("Cannot find the nested class `$builderClassName`.")
        }
    }

    @Test
    fun `'RejectionThrowable' class`() {
        assertDoc(expectedClassComment(), generatedClass)
    }

    @Test
    fun `'newBuilder' method`() {
        assertMethodDoc(NEW_BUILDER_METHOD_ABSTRACT, generatedClass, NEW_BUILDER)
    }

    @Test
    fun `'Builder' class under the 'RejectionThrowable'`() {
        assertDoc(expectedBuilderClassComment(), builderClass)
    }

    @Test
    fun `'build()' method of the 'Builder' class`() {
        assertMethodDoc(BUILD_METHOD_ABSTRACT, builderClass, BUILD)
    }

    @Test
    fun `property setters of the builder`() {
        assertMethodDoc(expectedFirstFieldComment(), builderClass, "setId")
        assertMethodDoc(expectedSecondFieldComment(), builderClass, "setRejectionMessage")
    }
}

private fun assertDoc(expectedText: String, element: PsiJavaDocumentedElement) {
    val docComment = element.docComment
    docComment.shouldNotBeNull()
    docComment.fullText() shouldBe expectedText
}

private fun assertMethodDoc(
    expectedComment: String,
    cls: PsiClass,
    methodName: String
) {
    val method = cls.findMethod(methodName)
    assertDoc(expectedComment, method)
}

private fun PsiClass.findMethod(methodName: String): PsiMethod =
    findMethodsByName(methodName, false).firstOrNull()
        ?: error("Cannot find the method `$methodName`.")

/**
 * Obtains the text of this Javadoc comment as a single line.
 *
 * Drops the comment delimiters and the leading asterisks of the lines,
 * and replaces each run of whitespace with a single space.
 * The values provided by [JavadocTestEnv] assume this form.
 */
private fun PsiDocComment.fullText(): String =
    text.removePrefix("/**")
        .removeSuffix("*/")
        .lines()
        .joinToString(" ") { it.trim().removePrefix("*") }
        .replace(whitespace, " ")
        .trim()

private val whitespace = Regex("\\s+")
