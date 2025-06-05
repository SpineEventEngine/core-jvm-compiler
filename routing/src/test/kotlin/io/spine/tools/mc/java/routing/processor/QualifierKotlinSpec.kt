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

package io.spine.tools.mc.java.routing.processor

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.spine.tools.mc.java.routing.processor.Qualifier.Companion.errorMsgPrefix
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCompilerApi::class)
@DisplayName("`Qualifier` in Kotlin should")
internal class QualifierKotlinSpec : RouteCompilationTest() {

    private val multicastEventRoute = kotlinFile("MulticastEventRoute", """

    package io.spine.given.devices
    
    import io.spine.given.devices.events.StatusReported
    import io.spine.server.projection.Projection
    import io.spine.server.route.Route

    class MulticastEventRoute : Projection<DeviceId, DeviceStatus, DeviceStatus.Builder>() {
    
        companion object {
                    
           /**
            * The routing function accepting the event class and
            * returning a set of IDs.   
            */
            @Route
            fun route(e: StatusReported) = setOf(e.getDevice())
        }
    }
    """.trimIndent())

    @Test
    fun `detect multi-cast event routes`() {
        compilation.sources = listOf(multicastEventRoute)

        val result = compilation.compileSilently()

        result.exitCode shouldBe OK
        result.messages shouldNotContain errorMsgPrefix
    }

    private val singleCastEventRoute = kotlinFile("SingleCastEventRoute", """

    package io.spine.given.devices
    
    import io.spine.given.devices.events.StatusReported
    import io.spine.server.projection.Projection
    import io.spine.server.route.Route

    class SingleCastEventRoute : Projection<DeviceId, DeviceStatus, DeviceStatus.Builder>() {
    
        companion object {
                    
           /**
            * The routing function accepting the event class and
            * returning single ID.   
            */
            @Route
            fun route(e: StatusReported) = e.getDevice()
        }
    }
    """.trimIndent())

    @Test
    fun `detect single-cast routing function`() {
        compilation.sources = listOf(singleCastEventRoute)

        val result = compilation.compileSilently()

        result.exitCode shouldBe OK
        result.messages shouldNotContain errorMsgPrefix
    }
}
