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

package io.spine.tools.core.jvm.gradle

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldStartWith
import io.spine.tools.core.jvm.gradle.given.createCoreJvmOptions
import io.spine.tools.core.jvm.gradle.given.newProject
import io.spine.tools.java.fs.DefaultJavaPaths
import java.io.File
import org.gradle.api.Project
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`TempArtifactDirs` should")
internal class TempArtifactDirsSpec {

    private fun dirsFor(project: Project): List<String> =
        TempArtifactDirs.getFor(project).map(File::toString)

    @Test
    fun `return the default 'generated' directory when no options are set`(
        @TempDir projectDir: File
    ) {
        val project = newProject(projectDir)
        project.createCoreJvmOptions()

        val dirs = dirsFor(project)

        dirs shouldHaveSize 1
        dirs[0] shouldStartWith project.projectDir.absolutePath
    }

    @Test
    fun `return directories set via options`(@TempDir projectDir: File) {
        val project = newProject(projectDir)
        val options = project.createCoreJvmOptions()
        options.tempArtifactDirs = listOf("foo-bar", "baz")

        val dirs = dirsFor(project)

        dirs shouldContain File("foo-bar").toString()
        dirs shouldContain File("baz").toString()
    }

    @Test
    fun `include the temp artifacts directory when it exists`(@TempDir projectDir: File) {
        val project = newProject(projectDir)
        project.createCoreJvmOptions()
        val tempArtifacts = DefaultJavaPaths.at(projectDir).tempArtifacts()
        check(tempArtifacts.mkdirs())

        val dirs = dirsFor(project)

        dirs shouldContain tempArtifacts.canonicalPath
    }

    @Test
    fun `include the temp artifacts directory of the root project`(
        @TempDir rootDir: File,
        @TempDir childDir: File
    ) {
        val root = newProject(rootDir)
        val child = newProject(childDir, parent = root)
        child.createCoreJvmOptions()
        val childTempArtifacts = DefaultJavaPaths.at(childDir).tempArtifacts()
        val rootTempArtifacts = DefaultJavaPaths.at(rootDir).tempArtifacts()
        check(childTempArtifacts.mkdirs())
        check(rootTempArtifacts.mkdirs())

        val dirs = dirsFor(child)

        dirs shouldContain childTempArtifacts.canonicalPath
        dirs shouldContain rootTempArtifacts.canonicalPath
    }
}
