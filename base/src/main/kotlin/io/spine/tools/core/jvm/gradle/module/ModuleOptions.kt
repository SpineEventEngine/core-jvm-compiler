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

package io.spine.tools.core.jvm.gradle.module

import io.spine.tools.core.jvm.gradle.CoreJvm
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories

/**
 * Options of a Spine-based project module.
 */
public abstract class ModuleOptions @Inject constructor(private val project: Project) {

    public val kind: Property<ModuleKind> = project.objects.property(ModuleKind::class.java)

    init {
        kind.convention(ModuleKind.SERVER)
        project.afterEvaluate {
            addRepositories()
            applyDependencies()
        }
    }
    private fun addRepositories() {
        val artifactRegistryBaseUrl = "https://europe-maven.pkg.dev/spine-event-engine"
        project.repositories {
            maven("$artifactRegistryBaseUrl/releases")
            maven("$artifactRegistryBaseUrl/snapshots")
            mavenCentral()
        }
    }

    private fun applyDependencies() {
        val kind = kind.get()
        val dependency = when (kind) {
            ModuleKind.SERVER -> CoreJvm.server
            ModuleKind.CLIENT -> CoreJvm.client
        }
        project.dependencies.add("implementation", dependency)
    }
}

/**
 * A kind of module to which the CoreJvm Gradle Plugin is applied.
 */
public enum class ModuleKind {

    /**
     * The module depends on `CoreJvm.server` API.
     */
    SERVER,

    /**
     * The module depends on `CoreJvm.client` API.
     */
    CLIENT
}
