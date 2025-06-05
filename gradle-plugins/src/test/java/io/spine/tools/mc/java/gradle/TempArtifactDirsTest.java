/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import io.spine.testing.TempDir;
import io.spine.tools.java.fs.DefaultJavaPaths;
import io.spine.tools.mc.java.gradle.given.StubProject;
import org.gradle.api.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.tools.mc.java.StandardRepos.applyStandard;
import static io.spine.tools.mc.java.gradle.Projects.getMcJava;
import static io.spine.tools.mc.java.gradle.given.ModelCompilerTestEnv.MC_JAVA_GRADLE_PLUGIN_ID;
import static io.spine.tools.mc.java.gradle.given.ModelCompilerTestEnv.newUuid;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("`DirsToClean` should")
class TempArtifactDirsTest {

    private Project project = null;
    private File projectDir = null;
    private McJavaOptions options = null;

    @BeforeEach
    void setUp() {
        projectDir = TempDir.forClass(TempArtifactDirsTest.class);
        project = StubProject.createAt(projectDir);
        var repositories = project.getRepositories();
        applyStandard(repositories);
        var plugins = project.getPluginManager();
        plugins.apply("java");
        plugins.apply("com.google.protobuf");
        plugins.apply(MC_JAVA_GRADLE_PLUGIN_ID);
        options = getMcJava(project);
    }

    @Nested
    @DisplayName("return")
    class Return {

        @Test
        @DisplayName("default value, if not set")
        void defaultValue() {
            var actualDirs = actualDirs();

            assertThat(actualDirs).hasSize(1);
            assertNotEmptyAndIsInProjectDir(actualDirs.get(0));
        }

        private void assertNotEmptyAndIsInProjectDir(String path) {
            assertThat(path.trim())
                    .isNotEmpty();
            assertThat(path)
                    .startsWith(project.getProjectDir()
                                       .getAbsolutePath());
        }

        @Test
        @DisplayName("list, if array is set")
        void list() {
            options.tempArtifactDirs = newArrayList(newUuid(), newUuid());

            var actualDirs = actualDirs();
            assertThat(actualDirs)
                    .isEqualTo(options.tempArtifactDirs);
        }
    }

    @Test
    @DisplayName("include `.spine` dir, if exists")
    void includeSpineDir() throws IOException {
        var defaultProject = DefaultJavaPaths.at(projectDir);
        var spineDir = defaultProject.tempArtifacts();
        assertTrue(spineDir.mkdir());
        var generatedDir =
                defaultProject.generated()
                              .path()
                              .toFile()
                              .getCanonicalPath();

        var dirsToClean = actualDirs();

        assertThat(dirsToClean)
                .containsAtLeast(spineDir.getCanonicalPath(), generatedDir);
    }

    private List<String> actualDirs() {
        var result =
                TempArtifactDirs.getFor(project)
                                .stream()
                                .map(File::toString)
                                .collect(toList());
        return result;
    }
}
