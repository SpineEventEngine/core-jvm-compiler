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

package io.spine.tools.core.jvm.gradle.plugins

import com.google.protobuf.Message
import io.spine.format.Format
import io.spine.tools.compiler.jvm.style.JavaCodeStyle
import io.spine.tools.compiler.settings.SettingsDirectory
import io.spine.tools.core.jvm.annotation.SettingsKt.annotationTypes
import io.spine.tools.core.jvm.annotation.settings
import io.spine.tools.core.jvm.gradle.AnnotationSettings
import io.spine.tools.core.jvm.gradle.CoreJvmOptions
import io.spine.tools.core.jvm.gradle.plugins.CoreJvmCompilerPlugins.API_ANNOTATIONS
import io.spine.tools.core.jvm.gradle.plugins.CoreJvmCompilerPlugins.COMPARABLE
import io.spine.tools.core.jvm.gradle.plugins.CoreJvmCompilerPlugins.ENTITY
import io.spine.tools.core.jvm.gradle.plugins.CoreJvmCompilerPlugins.MESSAGE_GROUP
import io.spine.tools.core.jvm.gradle.plugins.CoreJvmCompilerPlugins.SIGNAL
import io.spine.tools.core.jvm.gradle.plugins.CoreJvmCompilerPlugins.UUID
import io.spine.tools.core.jvm.gradle.plugins.WriteCompilerPluginsSettings.Companion.JAVA_CODE_STYLE_ID
import io.spine.type.toJson
import java.io.IOException
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import io.spine.tools.core.jvm.annotation.Settings as AnnotationPluginSettings

/**
 * A task that writes settings for CoreJvm plugins of the Spine Compiler.
 *
 * The [settingsDir] property defines the directory where settings files for
 * the CoreJvm Compiler plugins are stored.
 *
 * The settings to write are computed from [CoreJvmOptions] by a provider, so the task
 * action does not use the project model. This makes the task compatible with
 * the configuration cache.
 */
@DisableCachingByDefault(
    because = "Writing the settings files is cheaper than restoring them from the build cache."
)
@Suppress("unused") // Gradle creates a subtype for this class.
public abstract class WriteCompilerPluginsSettings : DefaultTask() {

    @get:OutputDirectory
    public abstract val settingsDir: DirectoryProperty

    /**
     * The settings of the CoreJvm Compiler plugins keyed by the IDs of the plugins.
     *
     * The settings are an input of the task, so Gradle reruns the task when
     * the user changes the [CoreJvmOptions] of the project.
     *
     * @see compilerPluginSettings
     */
    @get:Input
    internal abstract val settings: MapProperty<String, Message>

    /**
     * Writes the settings of each Compiler plugin to [settingsDir].
     */
    @TaskAction
    @Throws(IOException::class)
    public fun writeFiles() {
        val dir = settingsDirectory()
        // The settings are converted to JSON here rather than in the provider that supplies
        // the `settings`. The JSON printer loads the known Protobuf types via the context
        // class loader, which Gradle sets to the class loader of the plugin when running
        // task actions, but not when evaluating task inputs.
        settings.get().forEach { (id, message) ->
            dir.write(id, message)
        }
    }

    internal companion object {

        /**
         * The ID used by Validation plugin components to load the settings.
         */
        const val VALIDATION_SETTINGS_ID = "io.spine.validation.ValidationPlugin"

        /**
         * The ID for the Java code style settings.
         */
        val JAVA_CODE_STYLE_ID: String = JavaCodeStyle::class.java.canonicalName
    }
}

/**
 * Obtains an instance of [SettingsDirectory] to be used for writing files that
 * points to the directory specified by the [WriteCompilerPluginsSettings.settingsDir] property.
 */
private fun WriteCompilerPluginsSettings.settingsDirectory(): SettingsDirectory {
    val dir = settingsDir.get().asFile
    dir.mkdirs()
    return SettingsDirectory(dir.toPath())
}

/**
 * Writes the given instance of settings in [Format.ProtoJson] format using the [id].
 */
private fun SettingsDirectory.write(id: String, settings: Message) {
    write(consumerId = id, format = Format.ProtoJson, content = settings.toJson())
}

/**
 * Obtains the settings of the CoreJvm Compiler plugins specified by these options.
 *
 * Gradle calls this function when evaluating the inputs of [WriteCompilerPluginsSettings],
 * when the context class loader is not the one of the plugin. Therefore, the function must not
 * convert the settings to JSON, or use `KnownTypes` in any other way.
 *
 * @return The settings keyed by the IDs of the plugins.
 * @throws IllegalStateException If the `compiler` options are not set.
 */
internal fun CoreJvmOptions.compilerPluginSettings(): Map<String, Message> {
    val compilerOptions = checkNotNull(compiler) {
        "The `compiler` options are not set." +
                " `CoreJvmPlugin` must call `CoreJvmOptions.injectProject()` first."
    }
    val compilerSettings = compilerOptions.toProto()
    return mapOf(
        API_ANNOTATIONS to annotation.toProto(),
        ENTITY to compilerSettings.entities,
        SIGNAL to compilerSettings.signalSettings,
        MESSAGE_GROUP to compilerSettings.groupSettings,
        UUID to compilerSettings.uuids,
        COMPARABLE to compilerSettings.comparables,
        JAVA_CODE_STYLE_ID to style.get(),
    )
}

/**
 * Converts these settings into the settings of the API annotations plugin.
 */
private fun AnnotationSettings.toProto(): AnnotationPluginSettings {
    val javaType = types
    val classPatterns = internalClassPatterns.get()
    val methodNames = internalMethodNames.get()
    return settings {
        annotationTypes = annotationTypes {
            experimental = javaType.experimental.get()
            beta = javaType.beta.get()
            spi = javaType.spi.get()
            internal = javaType.internal.get()
        }
        internalClassPattern.addAll(classPatterns)
        internalMethodName.addAll(methodNames)
    }
}
