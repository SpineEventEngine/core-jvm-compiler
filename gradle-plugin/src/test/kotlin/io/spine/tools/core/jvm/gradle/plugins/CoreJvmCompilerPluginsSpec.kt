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

import io.kotest.matchers.shouldBe
import io.spine.tools.core.annotation.ApiAnnotationsPlugin
import io.spine.tools.core.jvm.comparable.ComparablePlugin
import io.spine.tools.core.jvm.entity.EntityPlugin
import io.spine.tools.core.jvm.marker.MarkerPlugin
import io.spine.tools.core.jvm.mgroup.MessageGroupPlugin
import io.spine.tools.core.jvm.signal.SignalPlugin
import io.spine.tools.core.jvm.signal.rejection.RThrowablePlugin
import io.spine.tools.core.jvm.uuid.UuidPlugin
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Guards [CoreJvmCompilerPlugins] against the drift from the classes it names.
 *
 * The production code of this module refers to the Compiler plugins by name
 * only, so a rename or a move of a plugin class would not fail its
 * compilation. The code-generation modules remain test dependencies of this
 * module for this suite to catch such a change.
 */
@DisplayName("`CoreJvmCompilerPlugins` should")
internal class CoreJvmCompilerPluginsSpec {

    @Test
    fun `name the plugin classes`() {
        CoreJvmCompilerPlugins.API_ANNOTATIONS shouldBe ApiAnnotationsPlugin::class.java.name
        CoreJvmCompilerPlugins.ENTITY shouldBe EntityPlugin::class.java.name
        CoreJvmCompilerPlugins.SIGNAL shouldBe SignalPlugin::class.java.name
        CoreJvmCompilerPlugins.REJECTION_THROWABLE shouldBe RThrowablePlugin::class.java.name
        CoreJvmCompilerPlugins.MARKER shouldBe MarkerPlugin::class.java.name
        CoreJvmCompilerPlugins.MESSAGE_GROUP shouldBe MessageGroupPlugin::class.java.name
        CoreJvmCompilerPlugins.UUID shouldBe UuidPlugin::class.java.name
        CoreJvmCompilerPlugins.COMPARABLE shouldBe ComparablePlugin::class.java.name
    }

    @Test
    fun `match the settings IDs of the plugins`() {
        CoreJvmCompilerPlugins.API_ANNOTATIONS shouldBe ApiAnnotationsPlugin.SETTINGS_ID
        CoreJvmCompilerPlugins.ENTITY shouldBe EntityPlugin.SETTINGS_ID
        CoreJvmCompilerPlugins.SIGNAL shouldBe SignalPlugin.SETTINGS_ID
        CoreJvmCompilerPlugins.MESSAGE_GROUP shouldBe MessageGroupPlugin.SETTINGS_ID
        CoreJvmCompilerPlugins.UUID shouldBe UuidPlugin.SETTINGS_ID
        CoreJvmCompilerPlugins.COMPARABLE shouldBe ComparablePlugin.SETTINGS_ID
    }
}
