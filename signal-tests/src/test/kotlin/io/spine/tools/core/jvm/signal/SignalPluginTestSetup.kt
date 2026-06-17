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

package io.spine.tools.core.jvm.signal

import com.google.protobuf.Descriptors.GenericDescriptor
import io.spine.base.MessageFile
import io.spine.tools.compiler.ast.FilePattern
import io.spine.tools.compiler.ast.FilePatternFactory.suffix
import io.spine.tools.core.jvm.PluginTestSetup
import io.spine.tools.core.jvm.settings.SignalSettings
import io.spine.tools.core.signal.given.command.BrokenIdCommand
import io.spine.tools.core.signal.given.command.RepeatedIdCommand
import java.nio.file.Path

/**
 * The abstract base for test suites of the Signal Plugin.
 */
@Suppress("UtilityClassWithPublicConstructor")
internal abstract class SignalPluginTestSetup : PluginTestSetup<SignalSettings>(
    SignalPlugin(),
    SignalPlugin.SETTINGS_ID
) {
    companion object {
        /**
         * The common parent directory for the generated Java code of signals.
         */
        const val JAVA_SRC_DIR = "io/spine/tools/core/signal/given"
        const val FIELD_CLASS_SIGNATURE = "public static final class Field"
    }

    /**
     * Creates an instance of [SignalSettings] as if it was created by McJava added to
     * a Gradle project.
     */
    override fun createSettings(projectDir: Path): SignalSettings {
        val codegenConfig = createCompilerSettings(projectDir)
        return codegenConfig.toProto().signalSettings
    }

    /**
     * Excludes the compile-fail fixtures from regular pipeline runs.
     *
     * [BrokenIdCommand] has an implicitly-required `Empty` target-entity ID field,
     * rejected at compile time and tested in `CommandIdErrorSpec`.
     *
     * [RepeatedIdCommand] has a `repeated` target-entity ID field, rejected at
     * compile time and tested in `UnsupportedCommandIdTypeErrorSpec`.
     */
    override fun defaultExclusions(): List<GenericDescriptor> =
        listOf(
            BrokenIdCommand.getDescriptor(),
            RepeatedIdCommand.getDescriptor(),
        )
}

/**
 * Creates [FilePattern] corresponding to this [MessageFile] type.
 */
internal fun MessageFile.pattern(): FilePattern = suffix(this.suffix())
