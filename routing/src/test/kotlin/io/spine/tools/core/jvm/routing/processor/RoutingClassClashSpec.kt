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

package io.spine.tools.core.jvm.routing.processor

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests reporting of entity classes for which routing classes would have the same name.
 *
 * @see NestedEntityRoutingSpec
 */
@OptIn(ExperimentalCompilerApi::class)
@DisplayName("`RouteProcessor` should detect routing classes with the same name")
internal class RoutingClassClashSpec : RouteCompilationTest() {

    @Test
    fun `and report the entity classes`() {
        compilation.sources = listOf(
            kotlinSource("Office", "class Office { $SCREEN }"),
            kotlinSource("Office_Screen", SCREEN.replace("class Screen", "class Office_Screen"))
        )
        val result = compilation.compileSilently()

        result.exitCode shouldBe COMPILATION_ERROR
        result.messages.let {
            it shouldContain "`$PACKAGE.Office.Screen`, `$PACKAGE.Office_Screen`"
            it shouldContain "`Office_Screen`"
            it shouldContain "rename"
            it shouldNotContain "FileAlreadyExistsException"
        }
    }

    @Test
    fun `only if the entity classes belong to the same package`() {
        val anotherPackage = "$PACKAGE.another"
        compilation.sources = listOf(
            kotlinSource("Office", "class Office { $SCREEN }"),
            kotlinSource(
                fileName = "Office_Screen",
                declarations = SCREEN.replace("class Screen", "class Office_Screen"),
                packageName = anotherPackage
            )
        )
        compilation.compileSilently().exitCode shouldBe OK
    }

    @Test
    fun `only if the entity classes have routes of the same kind`() {
        val routesCommands = "fun route(c: ReportStatus): DeviceId = c.device"
        val routesEvents = "fun route(e: StatusReported): DeviceId = e.device"
        compilation.sources = listOf(
            kotlinSource("Office", "class Office { ${screen("Screen", routesCommands)} }"),
            kotlinSource("Office_Screen", screen("Office_Screen", routesEvents))
        )
        compilation.compileSilently().exitCode shouldBe OK
    }
}
