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

package io.spine.tools.mc.java.gradle.plugins

import com.google.protobuf.Message
import io.spine.protodata.java.style.JavaCodeStyle
import io.spine.protodata.util.Format
import io.spine.protodata.settings.SettingsDirectory
import io.spine.tools.mc.annotation.ApiAnnotationsPlugin
import io.spine.tools.mc.java.annotation.SettingsKt.annotationTypes
import io.spine.tools.mc.java.annotation.settings
import io.spine.tools.mc.java.comparable.ComparablePlugin
import io.spine.tools.mc.java.entity.EntityPlugin
import io.spine.tools.mc.java.gradle.McJavaOptions
import io.spine.tools.mc.java.gradle.mcJava
import io.spine.tools.mc.java.gradle.plugins.WriteProtoDataSettings.Companion.JAVA_CODE_STYLE_ID
import io.spine.tools.mc.java.gradle.plugins.WriteProtoDataSettings.Companion.VALIDATION_SETTINGS_ID
import io.spine.tools.mc.java.mgroup.MessageGroupPlugin
import io.spine.tools.mc.java.settings.Combined
import io.spine.tools.mc.java.settings.signalSettings
import io.spine.tools.mc.java.signal.SignalPlugin
import io.spine.tools.mc.java.uuid.UuidPlugin
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
 * A task that writes settings for ProtoData.
 *
 * The [settingsDir] property defines the directory where settings files for
 * ProtoData plugins are stored.
 *
 * This task writes settings files for ProtoData components.
 */
@Suppress("unused") // Gradle creates a subtype for this class.
public abstract class WriteProtoDataSettings : DefaultTask() {

    @get:OutputDirectory
    public abstract val settingsDir: DirectoryProperty

    @get:Internal
    internal val options: McJavaOptions by lazy {
        project.mcJava
    }

    @get:Internal
    internal val codegenSettings by lazy {
        options.codegen!!.toProto()
    }

    @TaskAction
    @Throws(IOException::class)
    public fun writeFile() {
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
 * points to the directory specified by the [WriteProtoDataSettings.settingsDir] property.
 */
private fun WriteProtoDataSettings.settingsDirectory(): SettingsDirectory {
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
private fun WriteProtoDataSettings.forValidationPlugin(dir: SettingsDirectory) {
    val codegen = codegenSettings
    val signalSettings = codegen.signalSettings
    val markers = messageMarkers {
        signalSettings.let {
            commandPattern.addAll(it.commands.patternList)
            eventPattern.addAll(it.events.patternList)
            rejectionPattern.addAll(it.rejections.patternList)
        }
        entityOptionName.addAll(codegen.entityOptionsNames())
    }
    val settings = validationConfig {
        messageMarkers = markers
    }

    dir.write(VALIDATION_SETTINGS_ID, settings)
}

private fun Combined.entityOptionsNames(): Iterable<String> =
    entities.optionList.map { it.name }

private fun WriteProtoDataSettings.forAnnotationPlugin(dir: SettingsDirectory) {
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

private fun WriteProtoDataSettings.forEntityPlugin(dir: SettingsDirectory) {
    val entitySettings = codegenSettings.entities
    dir.write(EntityPlugin.SETTINGS_ID, entitySettings)
}

private fun WriteProtoDataSettings.forSignalPlugin(dir: SettingsDirectory) {
    val codegen = codegenSettings.signalSettings
    val signalSettings = signalSettings {
        commands = codegen.commands
        events = codegen.events
        rejections = codegen.rejections
    }
    dir.write(SignalPlugin.SETTINGS_ID, signalSettings)
}

private fun WriteProtoDataSettings.forMessageGroupPlugin(dir: SettingsDirectory) {
    val groupSettings = codegenSettings.groupSettings
    dir.write(MessageGroupPlugin.SETTINGS_ID, groupSettings)
}

private fun WriteProtoDataSettings.forUuidPlugin(dir: SettingsDirectory) {
    val uuidSettings = codegenSettings.uuids
    dir.write(UuidPlugin.SETTINGS_ID, uuidSettings)
}

private fun WriteProtoDataSettings.forComparablePlugin(dir: SettingsDirectory) {
    val settings = codegenSettings.comparables
    dir.write(ComparablePlugin.SETTINGS_ID, settings)
}

private fun WriteProtoDataSettings.forStyleFormattingPlugin(dir: SettingsDirectory) {
    val styleSettings = options.style.get()
    dir.write(JAVA_CODE_STYLE_ID, styleSettings)
}

/**
 * Writes the given instance of settings in [Format.PROTO_JSON] format using the [id].
 */
private fun SettingsDirectory.write(id: String, settings: Message) {
    write(id, Format.PROTO_JSON, settings.toJson())
}
