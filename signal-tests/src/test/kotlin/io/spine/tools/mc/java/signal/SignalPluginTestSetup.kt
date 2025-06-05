/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.tools.mc.java.signal

import io.spine.base.MessageFile
import io.spine.protodata.ast.FilePattern
import io.spine.protodata.ast.FilePatternFactory.suffix
import io.spine.tools.mc.java.PluginTestSetup
import io.spine.tools.mc.java.settings.SignalSettings
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
        const val FIELD_CLASS_SIGNATURE = "public static final class Field"
    }

    /**
     * Creates an instance of [SignalSettings] as if it was created by McJava added to
     * a Gradle project.
     */
    override fun createSettings(projectDir: Path): SignalSettings {
        val codegenConfig = createCodegenConfig(projectDir)
        return codegenConfig.toProto().signalSettings
    }
}

/**
 * Creates [FilePattern] corresponding to this [MessageFile] type.
 */
internal fun MessageFile.pattern(): FilePattern = suffix(suffix())
