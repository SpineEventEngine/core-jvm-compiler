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

package io.spine.tools.core.jvm.signal

import com.google.protobuf.Descriptors.GenericDescriptor
import io.spine.base.MessageFile
import io.spine.tools.compiler.ast.FilePattern
import io.spine.tools.compiler.ast.FilePatternFactory.suffix
import io.spine.tools.compiler.plugin.Plugin
import io.spine.tools.core.jvm.PluginTestSetup
import io.spine.tools.core.jvm.settings.SignalSettings
import io.spine.tools.core.signal.given.command.EmptyIdCommand
import io.spine.tools.core.signal.given.command.RepeatedIdCommand
import java.nio.file.Path

/**
 * The abstract base for test suites of the Signal Plugin.
 *
 * @param plugin The plugin to test. The suites of plugins other than [SignalPlugin],
 *   such as [RThrowablePlugin][io.spine.tools.core.jvm.signal.rejection.RThrowablePlugin],
 *   reuse the pipeline of this setup and its exclusions.
 */
@Suppress("UtilityClassWithPublicConstructor")
internal abstract class SignalPluginTestSetup(
    plugin: Plugin = SignalPlugin()
) : PluginTestSetup<SignalSettings>(
    plugin,
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
     * Creates an instance of [SignalSettings] as the CoreJvm Compiler would
     * create it when added to a Gradle project.
     */
    override fun createSettings(projectDir: Path): SignalSettings {
        val codegenConfig = createCompilerSettings(projectDir)
        return codegenConfig.toProto().signalSettings
    }

    /**
     * Excludes the compile-fail fixtures from regular pipeline runs.
     *
     * [EmptyIdCommand] has an implicitly-required `Empty` target-entity ID field,
     * rejected at compile time and tested in `CommandIdErrorSpec`.
     *
     * [RepeatedIdCommand] has a `repeated` target-entity ID field, rejected at
     * compile time and tested in `UnsupportedCommandIdTypeErrorSpec`.
     */
    override fun defaultExclusions(): List<GenericDescriptor> =
        listOf(
            EmptyIdCommand.getDescriptor(),
            RepeatedIdCommand.getDescriptor(),
        )
}

/**
 * Creates [FilePattern] corresponding to this [MessageFile] type.
 */
internal fun MessageFile.pattern(): FilePattern = suffix(this.suffix())
