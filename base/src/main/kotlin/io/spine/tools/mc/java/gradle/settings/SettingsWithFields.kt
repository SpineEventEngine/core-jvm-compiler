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

import com.google.protobuf.Message
import io.spine.tools.mc.java.field.AddFieldClass
import io.spine.tools.mc.java.settings.ActionMap
import io.spine.tools.mc.java.settings.BinaryClassName
import org.gradle.api.Project

/**
 * Code generation settings that include generation of
 * [field classes][io.spine.base.SubscribableField].
 *
 * Model Compiler generates the type-safe API for filtering messages by fields in queries
 * and subscriptions.
 *
 * @param S The Protobuf type reflecting a snapshot of these settings.
 *
 * @param project The project for which settings are created.
 * @param defaultActions Actions to be specified as the default value for the settings.
 */
public abstract class SettingsWithFields<S : Message> @JvmOverloads internal constructor(
    project: Project,
    defaultActions: ActionMap = mapOf()
) : SettingsWithActions<S>(project, defaultActions) {

    /**
     * Equips the field type with a superclass.
     *
     * @param className The canonical class name of an existing Java class.
     */
    public fun markFieldsAs(className: BinaryClassName) {
        useAction(AddFieldClass::class.java.name, className)
    }
}
