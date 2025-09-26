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

@file:Suppress("RemoveRedundantQualifierName") // To prevent IDEA replacing FQN imports.

import io.spine.dependency.build.Dokka
import io.spine.dependency.lib.Protobuf
import io.spine.dependency.local.Compiler
import io.spine.dependency.local.CoreJvm
import io.spine.dependency.local.Validation
import io.spine.gradle.RunBuild
import io.spine.gradle.RunGradle
import io.spine.gradle.publish.PublishingRepos
import io.spine.gradle.publish.SpinePublishing
import io.spine.gradle.publish.spinePublishing
import io.spine.gradle.repo.standardToSpineSdk
import io.spine.gradle.report.coverage.JacocoConfig
import io.spine.gradle.report.license.LicenseReporter
import io.spine.gradle.report.pom.PomGenerator
import java.time.Duration

buildscript {
    standardSpineSdkRepositories()

    val toolBase = io.spine.dependency.local.ToolBase
    val coreJava = io.spine.dependency.local.CoreJvm
    val validation = io.spine.dependency.local.Validation
    val logging = io.spine.dependency.local.Logging
    val compiler = io.spine.dependency.local.Compiler
    doForceVersions(configurations)
    configurations {
        all {
            exclude(group = "io.spine", module = "spine-logging-backend")
            resolutionStrategy {
                val configuration = this@all
                val strategy = this@resolutionStrategy
                io.spine.dependency.build.Ksp
                    .forceArtifacts(project, configuration, strategy)
                io.spine.dependency.lib.Kotlin
                    .forceArtifacts(project, configuration, strategy)
                io.spine.dependency.lib.Kotlin.StdLib
                    .forceArtifacts(project, configuration, strategy)
                force(
                    io.spine.dependency.lib.Kotlin.bom,
                    io.spine.dependency.local.Base.libForBuildScript,
                    io.spine.dependency.local.Reflect.lib,
                    toolBase.lib,
                    coreJava.server,
                    logging.lib,
                    logging.libJvm,
                    "${compiler.module}:${compiler.dogfoodingVersion}",

                    // Force ProtoData-compatible version because the build still uses McJava.
                    // See `classpath` dependencies below.
                    // When McJava is replaced with CoreJvmCompiler, these lines must be either removed
                    // or changed with the latest version of Validation.
                    "${validation.runtimeModule}:${validation.pdCompatibleVersion}",
                    "${validation.javaBundleModule}:${validation.pdCompatibleVersion}"
                )
            }
        }
    }
    dependencies {
        classpath(enforcedPlatform(io.spine.dependency.kotlinx.Coroutines.bom))
        classpath(enforcedPlatform(io.spine.dependency.lib.Grpc.bom))
        classpath(io.spine.dependency.local.ToolBase.jvmToolPlugins)
        classpath(coreJvmCompiler.pluginLib)
    }
}

plugins {
    idea
    errorprone
    jacoco
    `gradle-doctor`
    id("project-report")
    protobuf
    java
}

private object BuildSettings {
    const val TIMEOUT_MINUTES = 42L
}

spinePublishing {
    modules = productionModules.map { it.name }.toSet()
    destinations = PublishingRepos.run {
        setOf(
            cloudArtifactRegistry,
            gitHub("core-jvm-compiler"),
        )
    }
    artifactPrefix = "core-jvm-"
}

allprojects {
    apply(plugin = Dokka.GradlePlugin.id)
    apply(from = "$rootDir/version.gradle.kts")
    group = "io.spine.tools"
    version = extra["versionToPublish"]!!
    repositories.standardToSpineSdk()
}

subprojects {
    apply(plugin = "module")
    setupProtocArtifact()
}

JacocoConfig.applyTo(project)
PomGenerator.applyTo(project)
LicenseReporter.mergeAllReports(project)

/**
 * Collect `publishToMavenLocal` tasks for all subprojects that are specified for
 * publishing in the root project.
 */
val publishedModules: Set<String> = extensions.getByType<SpinePublishing>().modules

val localPublish by tasks.registering {
    val pubTasks = publishedModules.map { p ->
        val subProject = project(p)
        subProject.tasks["publishToMavenLocal"]
    }
    dependsOn(pubTasks)
}

/**
 * The build task executed under `tests` subdirectory.
 *
 * These tests depend on locally published artifacts.
 * It is similar to the dependency on such artifacts that `:gradle-plugins` module declares for
 * its tests. So, we depend on the `test` task of this module for simplicity.
 */
val integrationTests by tasks.registering(RunBuild::class) {
    directory = "$rootDir/tests"

    /** A timeout for the case of stalled child processes under Windows. */
    timeout.set(Duration.ofMinutes(BuildSettings.TIMEOUT_MINUTES))

    /** Run integration tests only after all regular tests pass in all modules. */
    subprojects.forEach {
        it.tasks.findByName("test")?.let { testTask ->
            this@registering.dependsOn(testTask)
        }
    }
    dependsOn(localPublish)
    doLast {
        val f = file("$directory/_out/error-out.txt")
        project.logger.error(f.readText())
    }
}

tasks.register("buildAll") {
    dependsOn(integrationTests)
}

val check by tasks.existing {
    dependsOn(integrationTests)
}

typealias Module = Project

/**
 * Specify `protoc` artifact for all the modules for simplicity.
 */
fun Module.setupProtocArtifact() {
    protobuf {
        protoc { artifact = Protobuf.compiler }
    }
}

apply(from = "version.gradle.kts")
val coreJvmCompilerVersion: String by extra

val prepareBuildPerformanceSettings by tasks.registering(Exec::class) {
    environment(
        "COMPILER_VERSION" to Compiler.version,
        "VALIDATION_VERSION" to Validation.version,
        "CORE_JVM_VERSION" to CoreJvm.version,
        "CORE_JVM_COMPILER_VERSION" to coreJvmCompilerVersion,
    )
    workingDir = File(rootDir, "BuildSpeed")
    commandLine("./substitute-settings.py")
}

tasks.register<RunGradle>("checkPerformance") {
    maxDurationMins = BuildSettings.TIMEOUT_MINUTES
    directory = "$rootDir/BuildSpeed"

    dependsOn(prepareBuildPerformanceSettings, localPublish)
    shouldRunAfter(check)

    // Do not run `BuildSpeed` until Validation is migrated to the Compiler.
    task("clean", "build")
}
