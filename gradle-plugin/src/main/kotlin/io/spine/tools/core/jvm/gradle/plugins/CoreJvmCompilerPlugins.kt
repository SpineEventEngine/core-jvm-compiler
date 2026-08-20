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

/**
 * The class names of the Compiler plugins provided by the modules of
 * the CoreJvm Compiler.
 *
 * The Compiler accepts plugin classes by name, and the settings ID of each
 * plugin equals its class name. Referring to the plugins by these constants —
 * rather than via class references — frees this module from compile-time
 * dependencies on the code-generation modules, whose classes run inside
 * the Compiler CLI classpath and not on the build classpath of consumer
 * projects.
 *
 * `CoreJvmCompilerPluginsSpec` verifies each constant against the class it
 * names, so a rename or a move of a plugin class fails the build of this
 * module instead of silently breaking the code generation of consumers.
 */
internal object CoreJvmCompilerPlugins {

    /** `ApiAnnotationsPlugin` of the `annotation` module. */
    const val API_ANNOTATIONS = "io.spine.tools.core.annotation.ApiAnnotationsPlugin"

    /** `EntityPlugin` of the `entity` module. */
    const val ENTITY = "io.spine.tools.core.jvm.entity.EntityPlugin"

    /** `SignalPlugin` of the `signal` module. */
    const val SIGNAL = "io.spine.tools.core.jvm.signal.SignalPlugin"

    /** `RThrowablePlugin` of the `signal` module. */
    const val REJECTION_THROWABLE = "io.spine.tools.core.jvm.signal.rejection.RThrowablePlugin"

    /** `MarkerPlugin` of the `marker` module. */
    const val MARKER = "io.spine.tools.core.jvm.marker.MarkerPlugin"

    /** `MessageGroupPlugin` of the `message-group` module. */
    const val MESSAGE_GROUP = "io.spine.tools.core.jvm.mgroup.MessageGroupPlugin"

    /** `UuidPlugin` of the `uuid` module. */
    const val UUID = "io.spine.tools.core.jvm.uuid.UuidPlugin"

    /** `ComparablePlugin` of the `comparable` module. */
    const val COMPARABLE = "io.spine.tools.core.jvm.comparable.ComparablePlugin"
}
