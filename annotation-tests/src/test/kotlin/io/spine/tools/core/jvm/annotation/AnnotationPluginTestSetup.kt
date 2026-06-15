/*
 * Copyright 2026, TeamDev. All rights reserved.
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

package io.spine.tools.core.jvm.annotation

import io.spine.annotation.Beta
import io.spine.annotation.Experimental
import io.spine.annotation.Internal
import io.spine.annotation.SPI
import io.spine.tools.core.annotation.ApiAnnotationsPlugin
import io.spine.tools.core.jvm.PluginTestSetup
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Abstract base for [ApiAnnotationsPlugin] tests.
 *
 * The setup mimics the settings written for the plugin by
 * the CoreJvm Compiler Gradle plugin: standard Spine annotation types and,
 * optionally, patterns for annotating classes and methods as `internal`.
 */
internal abstract class AnnotationPluginTestSetup(
    private val internalClassPatterns: List<String> = listOf(),
    private val internalMethodNames: List<String> = listOf()
) : PluginTestSetup<Settings>(ApiAnnotationsPlugin(), ApiAnnotationsPlugin.SETTINGS_ID) {

    /**
     * The directory with the Java code generated for the fixture proto files.
     */
    val generatedPackage: Path = Path("given/annotation")

    override fun createSettings(projectDir: Path): Settings = settings {
        annotationTypes = SettingsKt.annotationTypes {
            experimental = Experimental::class.java.canonicalName
            beta = Beta::class.java.canonicalName
            spi = SPI::class.java.canonicalName
            internal = Internal::class.java.canonicalName
        }
        internalClassPattern.addAll(this@AnnotationPluginTestSetup.internalClassPatterns)
        internalMethodName.addAll(this@AnnotationPluginTestSetup.internalMethodNames)
    }

    fun generateCode(projectDir: Path) {
        runPipeline(projectDir)
    }

    /**
     * Obtains the code of the Java file generated for the type with the given simple name.
     */
    fun code(simpleName: String): String =
        file(generatedPackage.resolve("$simpleName.java")).code()
}

/**
 * The `@Internal` annotation line added to the generated code.
 */
internal const val INTERNAL = "@io.spine.annotation.Internal"

/**
 * The `@Beta` annotation line added to the generated code.
 */
internal const val BETA = "@io.spine.annotation.Beta"

/**
 * The `@Experimental` annotation line added to the generated code.
 */
internal const val EXPERIMENTAL = "@io.spine.annotation.Experimental"

/**
 * The `@SPI` annotation line added to the generated code.
 */
internal const val SPI_ANNOTATION = "@io.spine.annotation.SPI"
