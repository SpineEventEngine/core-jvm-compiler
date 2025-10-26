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


@file:Suppress("TooManyFunctions")

package io.spine.tools.core.jvm.gradle.plugins

import com.google.protobuf.Message
import io.spine.format.Format
import io.spine.tools.compiler.jvm.style.JavaCodeStyle
import io.spine.tools.compiler.settings.SettingsDirectory
import io.spine.tools.core.annotation.ApiAnnotationsPlugin
import io.spine.tools.core.jvm.annotation.SettingsKt.annotationTypes
import io.spine.tools.core.jvm.annotation.settings
import io.spine.tools.core.jvm.comparable.ComparablePlugin
import io.spine.tools.core.jvm.entity.EntityPlugin
import io.spine.tools.core.jvm.gradle.CoreJvmOptions
import io.spine.tools.core.jvm.gradle.coreJvmOptions
import io.spine.tools.core.jvm.gradle.plugins.WriteCompilerPluginsSettings.Companion.JAVA_CODE_STYLE_ID
import io.spine.tools.core.jvm.gradle.plugins.WriteCompilerPluginsSettings.Companion.VALIDATION_SETTINGS_ID
import io.spine.tools.core.jvm.mgroup.MessageGroupPlugin
import io.spine.tools.core.jvm.settings.Combined
import io.spine.tools.core.jvm.settings.signalSettings
import io.spine.tools.core.jvm.signal.SignalPlugin
import io.spine.tools.core.jvm.uuid.UuidPlugin
import io.spine.type.toJson
import io.spine.validation.messageMarkers
import io.spine.validation.validationConfig
import java.io.IOException
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * A task that writes settings for CoreJvm plugins of the Spine Compiler.
 *
 * The [settingsDir] property defines the directory where settings files for
 * ProtoData plugins are stored.
 *
 * This task writes settings files for ProtoData components.
 */
@Suppress("unused") // Gradle creates a subtype for this class.
public abstract class WriteCompilerPluginsSettings : DefaultTask() {

    @get:OutputDirectory
    public abstract val settingsDir: DirectoryProperty

    @get:Internal
    internal val options: CoreJvmOptions by lazy {
        project.coreJvmOptions
    }

    @get:Internal
    internal val compilerSettings by lazy {
        options.compiler!!.toProto()
    }

    @TaskAction
    @Throws(IOException::class)
    public fun writeFiles() {
        val dir = settingsDirectory()
        forValidationPlugin(dir)
        forAnnotationPlugin(dir)
        forEntityPlugin(dir)
        forSignalPlugin(dir)
        forMessageGroupPlugin(dir)
        forUuidPlugin(dir)
        forComparablePlugin(dir)
        forStyleFormattingPlugin(dir)
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
 * Obtains an instance of [SettingsDirectory] to be used for writing files which
 * points to the directory specified by the [WriteCompilerPluginsSettings.settingsDir] property.
 */
private fun WriteCompilerPluginsSettings.settingsDirectory(): SettingsDirectory {
    val dir = project.file(settingsDir)
    dir.mkdirs()
    val settings = SettingsDirectory(dir.toPath())
    return settings
}

/**
 * Writes settings for Validation codegen.
 *
 * The settings are taken from McJava extension object and converted to
 * [io.spine.validation.ValidationConfig], which is later written as JSON file.
 */
private fun WriteCompilerPluginsSettings.forValidationPlugin(dir: SettingsDirectory) {
    val compilerSettings = compilerSettings
    val signalSettings = compilerSettings.signalSettings
    val markers = messageMarkers {
        signalSettings.let {
            commandPattern.addAll(it.commands.patternList)
            eventPattern.addAll(it.events.patternList)
            rejectionPattern.addAll(it.rejections.patternList)
        }
        entityOptionName.addAll(compilerSettings.entityOptionsNames())
    }
    val settings = validationConfig {
        messageMarkers = markers
    }

    dir.write(VALIDATION_SETTINGS_ID, settings)
}


private fun Combined.entityOptionsNames(): Iterable<String> =
    entities.optionList.map { it.name }

private fun WriteCompilerPluginsSettings.forAnnotationPlugin(dir: SettingsDirectory) {
    val annotation = options.annotation
    val proto = settings {
        val javaType = annotation.types
        annotationTypes = annotationTypes {
            experimental = javaType.experimental.get()
            beta = javaType.beta.get()
            spi = javaType.spi.get()
            internal = javaType.internal.get()
        }
        internalClassPattern.addAll(annotation.internalClassPatterns.get())
        internalMethodName.addAll(annotation.internalMethodNames.get())
    }
    dir.write(ApiAnnotationsPlugin.SETTINGS_ID, proto)
}

private fun WriteCompilerPluginsSettings.forEntityPlugin(dir: SettingsDirectory) {
    val entitySettings = compilerSettings.entities
    dir.write(EntityPlugin.SETTINGS_ID, entitySettings)
}

private fun WriteCompilerPluginsSettings.forSignalPlugin(dir: SettingsDirectory) {
    val codegen = compilerSettings.signalSettings
    val signalSettings = signalSettings {
        commands = codegen.commands
        events = codegen.events
        rejections = codegen.rejections
    }
    dir.write(SignalPlugin.SETTINGS_ID, signalSettings)
}

private fun WriteCompilerPluginsSettings.forMessageGroupPlugin(dir: SettingsDirectory) {
    val groupSettings = compilerSettings.groupSettings
    dir.write(MessageGroupPlugin.SETTINGS_ID, groupSettings)
}

private fun WriteCompilerPluginsSettings.forUuidPlugin(dir: SettingsDirectory) {
    val uuidSettings = compilerSettings.uuids
    dir.write(UuidPlugin.SETTINGS_ID, uuidSettings)
}

private fun WriteCompilerPluginsSettings.forComparablePlugin(dir: SettingsDirectory) {
    val settings = compilerSettings.comparables
    dir.write(ComparablePlugin.SETTINGS_ID, settings)
}

private fun WriteCompilerPluginsSettings.forStyleFormattingPlugin(dir: SettingsDirectory) {
    val styleSettings = options.style.get()
    dir.write(JAVA_CODE_STYLE_ID, styleSettings)
}

/**
 * Writes the given instance of settings in [Format.ProtoJson] format using the [id].
 */
private fun SettingsDirectory.write(id: String, settings: Message) {
    write(id, Format.ProtoJson, settings.toJson())
}
