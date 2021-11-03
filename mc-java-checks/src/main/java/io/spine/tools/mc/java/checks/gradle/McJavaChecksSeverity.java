/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.tools.mc.java.checks.gradle;

import com.google.common.annotations.VisibleForTesting;
import io.spine.logging.Logging;
import io.spine.tools.mc.checks.Severity;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.plugins.PluginContainer;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.tools.mc.java.checks.gradle.McJavaChecksExtension.getUseValidatingBuilderSeverity;

/**
 * The helper for the Spine-custom Error Prone checks configuration of the {@link Project}.
 *
 * <p>This class cannot configure the check severities without the Error Prone plugin applied to
 * the project.
 */
public final class McJavaChecksSeverity implements Logging {

    private static final String ERROR_PRONE_PLUGIN_ID = "net.ltgt.errorprone";

    private final Project project;
    private @Nullable Boolean hasErrorPronePlugin;

    private McJavaChecksSeverity(Project project) {
        this.project = project;
    }

    /**
     * Create the {@code SeverityConfigurer} instance for the given project.
     *
     * @param project
     *         the project
     * @return the {@code SeverityConfigurer} instance
     */
    public static McJavaChecksSeverity initFor(Project project) {
        checkNotNull(project);
        return new McJavaChecksSeverity(project);
    }

    /**
     * Adds the action configuring Spine Error Prone check severities to the
     * {@code projectEvaluated} stage of the project.
     */
    public void addConfigureSeverityAction() {
        Action<Gradle> configureCheckSeverity = g -> configureCheckSeverity();
        Gradle gradle = project.getGradle();
        gradle.projectsEvaluated(configureCheckSeverity);
    }

    /**
     * Adds command line flags necessary to configure Spine Error Prone check severities to all
     * {@code JavaCompile} tasks of the project.
     */
    private void configureCheckSeverity() {
        if (!hasErrorPronePlugin()) {
            _error().log("Cannot configure Spine Java Checks severity as the Error Prone " +
                                 "plugin is not applied to the project `%s`.", project.getName());
            return;
        }
        configureSeverities();
    }

    /**
     * Checks if the project has the Error Prone plugin applied.
     */
    private boolean hasErrorPronePlugin() {
        if (hasErrorPronePlugin == null) {
            PluginContainer appliedPlugins = project.getPlugins();
            hasErrorPronePlugin = appliedPlugins.hasPlugin(ERROR_PRONE_PLUGIN_ID);
        }
        return hasErrorPronePlugin;
    }

    /**
     * Configures default level of check severities.
     */
    private void configureSeverities() {
        //TODO:2021-10-12:alexander.yevsyukov: Take it from ModelCompiler/checks
        Severity severity = getUseValidatingBuilderSeverity(project);
        if (severity == null) {
            severity = Severity.WARN;
        }
        _debug().log(
                "Setting `UseValidatingBuilder` checker severity to `%s` for the project `%s`.",
                severity.name(), project.getName()
        );

        // String severityArg = "-Xep:UseValidatingBuilder:" + severity.name();
        ErrorProneOptionsAccess
                .of(project)
                // Pass already present check to demo the API.
                // Enumerate our custom checks doing the same later.
                .addArgs("-Xep:ReferenceEquality:ERROR"/*, severityArg*/);
    }

    /**
     * Allows to manually set the {@code hasErrorPronePlugin} property instead of
     * applying the plugin to a project when running tests.
     */
    @VisibleForTesting
    void setHasErrorPronePlugin(boolean value) {
        this.hasErrorPronePlugin = value;
    }
}
