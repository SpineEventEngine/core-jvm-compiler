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

@file:Suppress(
    "ClassNameDiffersFromFileName" /* false positive in IDEA */,
    "MissingPackageInfo" /* don't need them for these tests. */
)

package io.spine.tools.mc.java.routing.processor

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import io.kotest.matchers.shouldBe
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * This test suite checks positive scenarios of the compilation of
 * the [Route][io.spine.server.route.Route] annotation in the Java code.
 *
 * For error scenarios, please see [JavaRouteErrorSpec].
 *
 * @see JavaRouteErrorSpec
 */
@OptIn(ExperimentalCompilerApi::class)
@DisplayName("`RouteProcessor` should detect Java code errors")
internal class JavaRouteSpec : RouteCompilationTest() {

    private val twoRoutes = javaFile("TwoRoutes", """
        
    package io.spine.given.devices;
    
    import io.spine.given.devices.events.StatusReported;    
    import io.spine.given.devices.events.DeviceRegistered;    
    import io.spine.server.projection.Projection;
    import io.spine.server.route.Route;
        
    class TwoRoutes extends Projection<DeviceId, DeviceStatus, DeviceStatus.Builder> {
    
        @Route
        static DeviceId route(StatusReported event) {
            return event.getDevice();
        }
        
        @Route
        static DeviceId route(DeviceRegistered event) {
            return event.getDevice();
        } 
    }
    """.trimIndent())

    @Test
    fun `handle two routes`() {
        compilation.apply {
            sources = listOf(twoRoutes)
        }
        val result = compilation.compileSilently()

        result.exitCode shouldBe ExitCode.OK
    }
}
