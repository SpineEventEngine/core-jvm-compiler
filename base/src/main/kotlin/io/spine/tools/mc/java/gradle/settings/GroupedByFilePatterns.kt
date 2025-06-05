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

import com.google.common.collect.ImmutableSet
import com.google.protobuf.Message
import io.spine.protobuf.isDefault
import io.spine.protodata.ast.FilePattern
import io.spine.protodata.ast.FilePatternFactory
import io.spine.tools.gradle.Multiple
import io.spine.tools.mc.java.settings.ActionMap
import org.gradle.api.Project

/**
 * A configuration for code generation for a certain group of messages joined by a file pattern.
 *
 * @param S The Protobuf type reflecting a snapshot of these settings.
 *
 * @param project The project under which settings are created.
 * @param defaultActions Code generation actions to be executed for the grouped kind of messages.
 */
public abstract class GroupedByFilePatterns<S : Message> internal constructor(
    project: Project,
    defaultActions: ActionMap
) : SettingsWithFields<S>(project, defaultActions) {

    private val file = Multiple(project, FilePattern::class.java)

    /**
     * Sets up the default value for the file pattern.
     *
     * @param pattern
     *         the default value for the pattern.
     */
    public fun convention(pattern: FilePattern) {
        val defaultValue = if (pattern.isDefault()) {
            ImmutableSet.of()
        } else {
            ImmutableSet.of(pattern)
        }
        file.convention(defaultValue)
    }

    /**
     * Obtains the Gradle set property with the file pattern which matches messages in this group.
     */
    public fun patterns(): Set<FilePattern> {
        return file.get()
    }

    /**
     * Specifies a file pattern for this group of messages.
     *
     * Calling this method many times will extend the group to include more types. If a type is
     * declared in a file which matches at least one of the patterns, the type is included in
     * the group.
     *
     * In the example below, all messages declared in files, which either end with `"ids.proto"` or
     * contain the word `"identifiers"` will be included in the group.
     *
     * ```java
     * includeFiles(by().suffix("ids.proto"))
     * includeFiles(by().regex(".*identifiers.*"))
     * ```
     * @see by
     */
    public open fun includeFiles(pattern: FilePattern) {
        file.add(pattern)
    }

    /**
     * Obtains a factory of file patterns for selecting Protobuf files.
     *
     * @see includeFiles
     */
    public fun by(): FilePatternFactory = FilePatternFactory
}
