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

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.spine.server.route.setup.RoutingSetup
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests the names of the routing classes generated for entity classes
 * that are nested in other classes.
 *
 * @see RoutingClassClashSpec
 */
@OptIn(ExperimentalCompilerApi::class)
@DisplayName("`RouteProcessor` should name a routing class")
internal class NestedEntityRoutingSpec : RouteCompilationTest() {

    @Test
    fun `after the entity class if it is not nested`() {
        compile(kotlinSource("Screen", SCREEN))
            .shouldServe("ScreenEventRouting", entityClass = "Screen")
    }

    @Test
    fun `after the entity class and the outer class in Kotlin`() {
        compile(kotlinSource("Office", "class Office { $SCREEN }"))
            .shouldServe("Office_ScreenEventRouting", entityClass = "Office\$Screen")
    }

    @Test
    fun `after the entity class and the outer object in Kotlin`() {
        compile(kotlinSource("Office", "object Office { $SCREEN }"))
            .shouldServe("Office_ScreenEventRouting", entityClass = "Office\$Screen")
    }

    @Test
    fun `after the entity class and the outer class in Java`() {
        compile(javaSource("Office"))
            .shouldServe("Office_ScreenEventRouting", entityClass = "Office\$Screen")
    }

    @Test
    fun `after the entity class and all the outer classes`() {
        compile(kotlinSource("Office", "class Office { class Floor { $SCREEN } }"))
            .shouldServe("Office_Floor_ScreenEventRouting", entityClass = "Office\$Floor\$Screen")
    }

    @Test
    fun `in the same way for all kinds of routes`() {
        val result = compile(kotlinSource("Office", "class Office { $SCREEN }"))
        listOf("CommandRouting", "EventRouting", "StateUpdateRouting").forEach { suffix ->
            result.shouldServe("Office_Screen$suffix", entityClass = "Office\$Screen")
        }
    }

    @Test
    fun `differently for entity classes with the same simple name`() {
        val result = compile(
            kotlinSource("Screen", SCREEN),
            kotlinSource("Office", "class Office { $SCREEN }"),
            javaSource("Home")
        )
        result.run {
            shouldServe("ScreenEventRouting", entityClass = "Screen")
            shouldServe("Office_ScreenEventRouting", entityClass = "Office\$Screen")
            shouldServe("Home_ScreenEventRouting", entityClass = "Home\$Screen")
        }
    }

    private fun compile(vararg files: SourceFile): JvmCompilationResult {
        compilation.sources = files.toList()
        val result = compilation.compileSilently()
        withClue(result.messages) {
            result.exitCode shouldBe ExitCode.OK
        }
        return result
    }

    /**
     * Asserts that the routing class with the given name serves the given entity class.
     */
    private fun JvmCompilationResult.shouldServe(routingClass: String, entityClass: String) {
        val cls = classLoader.loadClass("$PACKAGE.$routingClass")
        val setup = cls.getDeclaredConstructor().newInstance() as RoutingSetup<*, *, *, *, *>
        setup.entityClass().name shouldBe "$PACKAGE.$entityClass"
    }
}
