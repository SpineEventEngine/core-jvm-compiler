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

package io.spine.tools.mc.java.gradle.plugins

import io.spine.io.Delete.deleteRecursively
import io.spine.tools.gradle.task.BaseTaskName
import io.spine.tools.gradle.task.GradleTask
import io.spine.tools.mc.java.gradle.McJavaTaskName.Companion.preClean
import io.spine.tools.mc.java.gradle.TempArtifactDirs
import java.io.File
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * Plugin which performs additional cleanup of the Spine-generated folders.
 *
 * Adds a custom `preClean` task, which is executed before the `clean` task.
 */
public class CleaningPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val preCleanAction = PreCleanAction(project)
        val preCleanTask = GradleTask.newBuilder(preClean, preCleanAction)
            .insertBeforeTask(BaseTaskName.clean)
            .applyNowTo(project)
        project.logger.debug("Pre-clean phase initialized: `{}`.", preCleanTask)
    }
}

/**
 * Recursively deletes [temp. artifact directories][TempArtifactDirs] in
 * the given project.
 */
private class PreCleanAction(private val project: Project) : Action<Task> {

    override fun execute(task: Task) {
        val logger = project.logger
        val dirsToClean = TempArtifactDirs.getFor(project)
        if (logger.isDebugEnabled) {
            val dirs = dirsToClean.joinToString()
            logger.debug("Pre-clean: deleting the directories (`{}`).", dirs)
        }
        dirsToClean.map(File::toPath)
            .forEach { dir ->
                logger.debug("Deleting directory `{}`...", dir)
                deleteRecursively(dir)
            }
    }
}
