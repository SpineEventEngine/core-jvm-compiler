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

package io.spine.tools.core.jvm.gradle.given

import io.spine.tools.core.jvm.gradle.CoreJvmOptions
import io.spine.tools.gradle.root.RootPlugin
import io.spine.tools.gradle.root.rootExtension
import java.io.File
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

/**
 * Creates a Gradle project in the given directory.
 */
internal fun newProject(dir: File? = null, parent: Project? = null): Project {
    val builder = ProjectBuilder.builder()
    dir?.let { builder.withProjectDir(it) }
    parent?.let { builder.withParent(it) }
    return builder.build()
}

/**
 * Registers the `coreJvm` extension in this project the way
 * the CoreJvm Gradle plugin does it.
 */
internal fun Project.createCoreJvmOptions(): CoreJvmOptions {
    pluginManager.apply(RootPlugin::class.java)
    return rootExtension.extensions.create(CoreJvmOptions.NAME, CoreJvmOptions::class.java)
}
