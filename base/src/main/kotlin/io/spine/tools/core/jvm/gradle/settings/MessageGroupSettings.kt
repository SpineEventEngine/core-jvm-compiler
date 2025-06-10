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

package io.spine.tools.core.jvm.gradle.settings

import com.google.common.base.MoreObjects
import io.spine.tools.mc.java.settings.MessageGroup
import io.spine.tools.mc.java.settings.Pattern
import io.spine.tools.mc.java.settings.messageGroup
import io.spine.type.shortDebugString
import org.gradle.api.Project

/**
 * Codegen settings for messages which match a certain pattern.
 *
 * @param project The project for which settings are created.
 * @property pattern The pattern to select message types.
 *
 * @constructor Creates an instance of settings for the given project and the specified pattern.
 *
 * @see CodegenSettings.forMessages
 */
public class MessageGroupSettings internal constructor(
    project: Project,
    private val pattern: Pattern
) : SettingsWithFields<MessageGroup>(project) {

    override fun toProto(): MessageGroup {
        return messageGroup {
            pattern = this@MessageGroupSettings.pattern
            actions = actions()
        }
    }

    override fun toString(): String {
        return MoreObjects.toStringHelper(this)
            .add("pattern", pattern.shortDebugString())
            .toString()
    }
}
