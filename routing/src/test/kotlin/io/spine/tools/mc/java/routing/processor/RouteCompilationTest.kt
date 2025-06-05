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

import com.google.auto.service.AutoService
import com.google.protobuf.MessageOrBuilder
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.configureKsp
import io.spine.base.CommandMessage
import io.spine.core.EventContext
import io.spine.given.devices.Device
import io.spine.logging.WithLogging
import io.spine.logging.testing.ConsoleTap
import io.spine.server.route.Route
import io.spine.validate.ValidatingBuilder
import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach

/**
 * Abstract base for tests checking handling compilation of the [Route] annotation.
 *
 * The tests use types from the Protobuf code generated for the `given.devices` proto package.
 */
@OptIn(ExperimentalCompilerApi::class)
sealed class RouteCompilationTest {

    companion object {

        /**
         * Suppress excessive console output produced by [KotlinCompilation.compile].
         *
         * @see <a href="https://github.com/google/ksp/issues/1687">Related issue</a>
         * @see KotlinCompilation.compileSilently
         */
        @BeforeAll
        @JvmStatic
        fun redirectStreams() {
            ConsoleTap.install()
        }
    }
    protected lateinit var compilation: KotlinCompilation

    @BeforeEach
    fun prepareCompilation() {
        compilation = KotlinCompilation()
        compilation.jvmTarget = JavaVersion.VERSION_17.toString()

        val dependencyJars = setOf(
            AutoService::class.java,
            MessageOrBuilder::class.java, // Protobuf
            CommandMessage::class.java, // Base
            WithLogging::class.java, // Logging library.
            ValidatingBuilder::class.java, // Validation runtime
            EventContext::class.java, // CoreJava.core
            Route::class.java, // CoreJava.server
            RouteProcessorProvider::class.java, // RouteProcessor
            Device::class.java // Compiled protos
        ).map { it.classpathFile() }

        compilation.apply {
            javaPackagePrefix = "io.spine.routing.given"
            configureKsp (useKsp2 = true) {
                symbolProcessorProviders += RouteProcessorProvider()
            }
            classpaths = classpaths + dependencyJars
        }
    }
}
