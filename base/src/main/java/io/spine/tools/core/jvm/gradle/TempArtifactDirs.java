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

package io.spine.tools.core.jvm.gradle;

import com.google.common.collect.ImmutableList;
import io.spine.logging.Logger;
import io.spine.logging.LoggingFactory;
import io.spine.tools.java.fs.DefaultJavaPaths;
import org.gradle.api.Project;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static io.spine.tools.core.jvm.gradle.CoreJvmOptions.def;
import static io.spine.tools.core.jvm.gradle.Projects.getCoreJvmOptions;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static kotlin.jvm.JvmClassMappingKt.getKotlinClass;

/**
 * Calculates directories to be cleaned for a given project.
 */
public class TempArtifactDirs {

    private static final Logger log = LoggingFactory.getLogger(
            getKotlinClass(TempArtifactDirs.class)
    );

    /** Prevents instantiation of this utility class. */
    private TempArtifactDirs() {
    }

    /**
     * Obtains directories to be removed in the given project when the {@code preClean} task
     * is executed.
     */
    public static List<File> getFor(Project project) {
        ImmutableList.Builder<File> result = ImmutableList.builder();
        result.addAll(tempArtifactDirsOf(project));
        var dirs = fromOptionsOf(project);
        if (!dirs.isEmpty()) {
            log.atDebug()
               .log(() -> format("Found %d directories to clean: `%s`.", dirs.size(), dirs));
            result.addAll(dirs);
        } else {
            var defaultValue = def(project).generated().toString();
            log.atDebug()
               .log(() -> format("Default directory to clean: `%s`.", defaultValue));
            result.add(new File(defaultValue));
        }
        return result.build();
    }

    private static List<File> fromOptionsOf(Project project) {
        var options = getCoreJvmOptions(project);
        var dirs = options.tempArtifactDirs.stream()
                .map(File::new)
                .collect(toList());
        return dirs;
    }

    private static List<File> tempArtifactDirsOf(Project project) {
        List<File> result = new ArrayList<>();
        var tempArtifactDir = tempArtifactsDirOf(project);
        var tempArtifactDirOfRoot = tempArtifactsDirOf(project.getRootProject());
        if (tempArtifactDir != null) {
            result.add(tempArtifactDir);
            if (tempArtifactDirOfRoot != null
                    && !areSame(tempArtifactDir, tempArtifactDirOfRoot)) {
                result.add(tempArtifactDirOfRoot);
            }
        }
        return result;
    }

    private static @Nullable File tempArtifactsDirOf(Project project) {
        var projectDir = canonicalDirOf(project);
        var tempArtifactsDir = DefaultJavaPaths.at(projectDir)
                                               .tempArtifacts();
        if (tempArtifactsDir.exists()) {
            return tempArtifactsDir;
        } else {
            return null;
        }
    }

    private static File canonicalDirOf(Project project) {
        File result;
        var projectDir = project.getProjectDir();
        try {
            result = projectDir.getCanonicalFile();
        } catch (IOException e) {
            throw newIllegalStateException(
                    e, "Unable to obtain canonical project directory `%s`.", projectDir
            );
        }
        return result;
    }

    private static boolean areSame(File f1, File f2) {
        var p1 = f1.toPath();
        var p2 = f2.toPath();
        try {
            return Files.isSameFile(p1, p2);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
