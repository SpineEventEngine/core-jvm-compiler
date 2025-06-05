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

package io.spine.tools.mc.java.gradle.settings

import com.google.common.annotations.VisibleForTesting
import io.spine.annotation.Internal
import io.spine.option.OptionsProto
import io.spine.query.EntityStateField
import io.spine.tools.mc.java.settings.ActionMap
import io.spine.tools.mc.java.settings.Entities
import io.spine.tools.mc.java.settings.entities
import io.spine.tools.mc.java.settings.noParameter
import io.spine.tools.proto.code.ProtoOption
import io.spine.tools.proto.code.protoOption
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

/**
 * Configuration for entity state types' code generation.
 *
 * @param project The project for which settings are created.
 *
 * @see CodegenSettings.forEntities
 */
public class EntitySettings @VisibleForTesting public constructor(project: Project) :
    SettingsWithFields<Entities>(project, DEFAULT_ACTIONS) {

    /**
     * The Protobuf options which mark entity states.
     *
     * By default, the `(entity)` option is used.
     */
    @get:Internal
    public val options: SetProperty<String>

    private val generateQueries: Property<Boolean>

    init {
        markFieldsAs(EntityStateField::class.java.canonicalName)
        options = project.objects.setProperty(String::class.java)
        options.convention(
            setOf(OptionsProto.entity.descriptor.name)
        )
        generateQueries = project.objects.property(Boolean::class.java)
        generateQueries.convention(true)
    }

    /**
     * Enables type-safe query API generation for entity states.
     */
    public fun generateQueries() {
        generateQueries.set(true)
    }

    /**
     * Disables type-safe query API generation for entity states.
     */
    public fun skipQueries() {
        generateQueries.set(false)
    }

    override fun toProto(): Entities {
        return entities {
            option.addAll(options())
            generateQueries = this@EntitySettings.generateQueries.get()
            this@entities.actions = actions()
        }
    }

    private fun options(): List<ProtoOption> {
        return options.get().map { protoOption { name = it } }
    }

    public companion object {

        /**
         * Names of render action classes applied by default to entity states.
         */
        @VisibleForTesting
        public val DEFAULT_ACTIONS: ActionMap = mapOf(
            "io.spine.tools.mc.java.field.AddFieldClass" to noParameter,
            "io.spine.tools.mc.java.entity.column.AddColumnClass" to noParameter,
            "io.spine.tools.mc.java.entity.query.AddQuerySupport" to noParameter,
            "io.spine.tools.mc.java.entity.ImplementEntityState" to noParameter
        )
    }
}
