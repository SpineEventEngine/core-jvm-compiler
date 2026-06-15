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

package io.spine.tools.core.jvm.gradle.module

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.spine.tools.core.jvm.gradle.CoreJvm
import io.spine.tools.core.jvm.gradle.CoreJvmOptions
import io.spine.tools.core.jvm.gradle.given.createCoreJvmOptions
import io.spine.tools.core.jvm.gradle.given.newProject
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.project.ProjectInternal
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`ModuleOptions` should")
internal class ModuleOptionsSpec {

    private lateinit var project: Project
    private lateinit var options: CoreJvmOptions

    @BeforeEach
    fun createOptions() {
        project = newProject()
        // The `implementation` configuration must exist when `ModuleOptions`
        // adds the dependency on project evaluation.
        project.pluginManager.apply("java")
        options = project.createCoreJvmOptions()
    }

    @Test
    fun `use the 'SERVER' module kind by default`() {
        options.module.kind.get() shouldBe ModuleKind.SERVER
    }

    @Test
    fun `add Spine artifact repositories`() {
        options.module.shouldNotBeNull()
        val urls = project.repositories
            .filterIsInstance<MavenArtifactRepository>()
            .map { it.url.toString() }
        urls shouldContain ArtifactRegistry.releases
        urls shouldContain ArtifactRegistry.snapshots
    }

    @Test
    fun `allow changing the module kind`() {
        options.module.kind.set(ModuleKind.CLIENT)
        options.module.kind.get() shouldBe ModuleKind.CLIENT
    }

    @Test
    fun `add the 'spine-server' dependency for the 'SERVER' module kind`() {
        options.module.shouldNotBeNull()
        (project as ProjectInternal).evaluate()
        implementationDependencies() shouldContain CoreJvm.server.artifact.coordinates
    }

    @Test
    fun `add the 'spine-client' dependency for the 'CLIENT' module kind`() {
        options.module.kind.set(ModuleKind.CLIENT)
        (project as ProjectInternal).evaluate()
        implementationDependencies() shouldContain CoreJvm.client.artifact.coordinates
    }

    /**
     * Obtains Maven coordinates of the dependencies added to
     * the `implementation` configuration of the [project].
     */
    private fun implementationDependencies(): List<String> =
        project.configurations
            .getByName("implementation")
            .dependencies
            .map { "${it.group}:${it.name}:${it.version}" }
}
