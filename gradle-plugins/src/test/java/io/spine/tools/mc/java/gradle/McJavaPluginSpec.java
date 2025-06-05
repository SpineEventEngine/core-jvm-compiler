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

package io.spine.tools.mc.java.gradle;

import io.spine.tools.gradle.task.TaskName;
import io.spine.tools.gradle.testing.GradleProject;
import io.spine.tools.mc.java.gradle.given.StubProject;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.tools.gradle.task.BaseTaskName.clean;
import static io.spine.tools.gradle.testing.GradleTruth.assertThat;
import static io.spine.tools.mc.java.gradle.GradleProjects.evaluate;
import static io.spine.tools.mc.java.gradle.McJavaTaskName.preClean;
import static io.spine.tools.mc.java.gradle.given.ModelCompilerTestEnv.MC_JAVA_GRADLE_PLUGIN_ID;

@DisplayName("`McJavaPlugin` should")
class McJavaPluginSpec {

    private static TaskContainer tasks = null;

    @BeforeAll
    static void createProjectWithPlugin() {
        var project = StubProject.createFor(McJavaPluginSpec.class)
                                 .withMavenRepositories()
                                 .get();
        var plugins = project.getPluginManager();
        plugins.apply(GradleProject.javaPlugin);
        plugins.apply("com.google.protobuf");
        plugins.apply(MC_JAVA_GRADLE_PLUGIN_ID);

        evaluate(project);

        tasks = project.getTasks();
    }

    @Nested
    @DisplayName("should add a task")
    class AddTask {

        @Test
        void preClean() {
            assertThat(task(clean)).dependsOn(task(preClean))
                                   .isTrue();
        }

        private static Task task(TaskName taskName) {
            var task = tasks.getByName(taskName.name());
            assertThat(task).isNotNull();
            return task;
        }
    }
}
