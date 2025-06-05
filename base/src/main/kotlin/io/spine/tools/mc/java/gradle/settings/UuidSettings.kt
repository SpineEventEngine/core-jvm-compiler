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
import io.spine.base.UuidValue
import io.spine.protodata.java.render.ImplementInterface
import io.spine.protodata.java.render.superInterface
import io.spine.tools.mc.java.settings.ActionMap
import io.spine.tools.mc.java.settings.Uuids
import io.spine.tools.mc.java.settings.noParameter
import io.spine.tools.mc.java.settings.uuids
import org.gradle.api.Project

/**
 * Settings for code generation for messages that qualify as [io.spine.base.UuidValue].
 */
public class UuidSettings(project: Project) : SettingsWithActions<Uuids>(project, DEFAULT_ACTIONS) {

    override fun toProto(): Uuids {
        return uuids {
            this@uuids.actions = actions()
        }
    }

    public companion object {

        /**
         * The name of the default codegen action applied to [io.spine.base.UuidValue]s.
         */
        @VisibleForTesting
        public val DEFAULT_ACTIONS: ActionMap = mapOf(
            ImplementInterface::class.java.name to
                    superInterface { name = UuidValue::class.java.name },

            "io.spine.tools.mc.java.uuid.AddFactoryMethods" to noParameter
        )
    }
}
