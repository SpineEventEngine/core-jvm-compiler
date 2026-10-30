/*
 * Copyright 2026 CodeMatters, Lda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

@file:Suppress("RemoveRedundantQualifierName") // To prevent IDEA replacing FQN imports.

import io.spine.dependency.build.Dokka
import io.spine.dependency.isDokka
import io.spine.dependency.local.Compiler
import io.spine.dependency.local.CoreJvm
import io.spine.dependency.local.Spine
import io.spine.dependency.local.Validation
import io.spine.gradle.RunBuild
import io.spine.gradle.RunGradle
import io.spine.gradle.SpineTaskGroup
import io.spine.gradle.publish.PublishingRepos
import io.spine.gradle.publish.SpinePublishing
import io.spine.gradle.publish.spinePublishing
import io.spine.gradle.repo.standardToSpineSdk
import io.spine.gradle.report.coverage.KoverConfig
import io.spine.gradle.report.license.LicenseReporter
import io.spine.gradle.report.pom.PomGenerator
import java.time.Duration

buildscript {
    standardSpineSdkRepositories()
    val base = io.spine.dependency.local.Base
    val compiler = io.spine.dependency.local.Compiler
    val jackson = io.spine.dependency.lib.Jackson
    val kotlin = io.spine.dependency.lib.Kotlin
    val logging = io.spine.dependency.local.Logging
    val time = io.spine.dependency.local.Time
    val toolBase = io.spine.dependency.local.ToolBase
    val validation = io.spine.dependency.local.Validation
    doForceVersions(configurations)
    configurations {
        all {
            exclude(group = "io.spine", module = "spine-logging-backend")
            resolutionStrategy {
                val cfg = this@all
                val rs = this@resolutionStrategy
                kotlin.forceArtifacts(project, cfg, rs)
                io.spine.dependency.lib.Kotlin.StdLib.forceArtifacts(project, cfg, rs)
                jackson.forceArtifacts(project, cfg, rs)
                io.spine.dependency.lib.Jackson.DataFormat.forceArtifacts(project, cfg, rs)
                io.spine.dependency.lib.Jackson.DataType.forceArtifacts(project, cfg, rs)
                io.spine.dependency.lib.JacksonV2.Core.forceArtifacts(project, cfg, rs)
                io.spine.dependency.lib.JacksonV2.DataFormat.forceArtifacts(project, cfg, rs)
                io.spine.dependency.lib.JacksonV2.DataType.forceArtifacts(project, cfg, rs)
                io.spine.dependency.lib.JacksonV2.Module.forceArtifacts(project, cfg, rs)
                io.spine.dependency.lib.JacksonV2.Junior.forceArtifacts(project, cfg, rs)

                force(
                    io.spine.dependency.lib.Caffeine.lib,
                    kotlin.bom,
                    jackson.bom,
                    logging.lib,
                    logging.libJvm,
                    logging.grpcContext,
                    validation.runtime,
                    jackson.annotations,
                    base.annotations,
                    base.libForBuildScript,
                    base.environment,
                    base.format,
                    io.spine.dependency.local.Reflect.lib,
                    time.lib,
                    time.javaExtensions,
                    compiler.api,
                    compiler.params,
                    compiler.gradleApi,
                    compiler.pluginLib,
                    // `jvm-tool-plugins` and the Compiler Gradle plugin may depend on
                    // different ToolBase versions, which `failOnVersionConflict()` rejects.
                    toolBase.fs,
                    toolBase.code,
                    toolBase.javaCode,
                    toolBase.kotlinCode,
                    toolBase.protoCode,
                    toolBase.classicCodegen,
                    toolBase.pluginBase,
                    toolBase.jvmTools,
                    toolBase.gradlePluginApi,
                    toolBase.intellijPlatform,
                    toolBase.intellijPlatformJava,
                    toolBase.protobufSetupPlugins,
                    toolBase.psiJava,
                )
            }
        }
    }
    dependencies {
        classpath(enforcedPlatform(io.spine.dependency.kotlinx.Coroutines.bom))
        classpath(enforcedPlatform(io.spine.dependency.lib.Grpc.bom))
        classpath(toolBase.jvmToolPluginDogfooding)
        classpath(compiler.pluginLib)
        classpath(coreJvmCompiler.gradlePlugin)
    }
}

plugins {
    idea
    errorprone
    id("org.jetbrains.kotlinx.kover")
    `gradle-doctor`
    id("project-report")
    protobuf
    java
}

private object BuildSettings {
    const val TIMEOUT_MINUTES = 42L
}

spinePublishing {
    modulesWithCustomPublishing = setOf(
        "compiler-plugins",
        "gradle-plugin",
    )
    destinations = PublishingRepos.run {
        setOf(
            cloudArtifactRegistry,
            gitHub("core-jvm-compiler"),
        )
    }
    toolArtifactPrefix = "core-jvm-"
}

allprojects {
    apply(plugin = Dokka.GradlePlugin.id)
    apply(from = "$rootDir/version.gradle.kts")
    group = Spine.toolsGroup
    version = extra["versionToPublish"]!!
    repositories.standardToSpineSdk()
    configurations.all {
        if (isDokka) {
            return@all
        }
        resolutionStrategy {
            // Applies to every module — both `module`- and `test-module`-based
            // ones. Floor artifacts request the pre-refresh versions of these,
            // tripping `failOnVersionConflict()`; the Protobuf runtime must
            // additionally never be older than the refreshed gencode.
            force(
                io.spine.dependency.lib.Protobuf.javaLib,
                io.spine.dependency.kotlinx.Coroutines.bom,
                io.spine.dependency.kotlinx.AtomicFu.lib,
            )
        }
    }
}

subprojects {
    tasks.withType<com.google.protobuf.gradle.GenerateProtoTask>().configureEach {
        outputs.cacheIf(
            "Protoc plugins used in this repository — ProtoTap in `*-tests`" +
                    " modules and the Spine Compiler protoc plugin elsewhere —" +
                    " write captures (`CodeGeneratorRequest.binpb`, descriptor" +
                    " references) as side effects without declaring them as task" +
                    " outputs. A build-cache hit restores only the declared" +
                    " outputs, breaking the downstream codegen and" +
                    " `PipelineSetup`-based test suites. Always run protoc."
        ) { false }
    }
}

KoverConfig.applyTo(rootProject)
PomGenerator.applyTo(project)
LicenseReporter.mergeAllReports(project)

/**
 * Collect `publishToMavenLocal` tasks for all subprojects that are specified for
 * publishing in the root project.
 */
val publishedModules: Set<Project> = extensions.getByType<SpinePublishing>().projectsToPublish()

val localPublish = tasks.register("localPublish") {
    val pubTasks = publishedModules.map { p ->
        p.tasks["publishToMavenLocal"]
    }
    dependsOn(pubTasks)
}

/**
 * The build task executed under `tests` subdirectory.
 *
 * These tests depend on locally published artifacts.
 * It is similar to the dependency on such artifacts that `:gradle-plugin` module declares
 * for its tests. So, we depend on the `test` task of this module for simplicity.
 */
val integrationTests = tasks.register<RunBuild>("integrationTests") {
    group = SpineTaskGroup.name
    description = "Runs the integration tests of the `tests` build" +
            " against the locally published artifacts"
    directory = "$rootDir/tests"

    /** A timeout for the case of stalled child processes under Windows. */
    timeout.set(Duration.ofMinutes(BuildSettings.TIMEOUT_MINUTES))

    /*
     * Run integration tests only after all regular tests pass in all modules.
     *
     * The dependency is on live task collections rather than on task instances, so
     * that Gradle resolves it when it builds the task graph — whenever this block
     * happens to run. Looking the tasks up here, by name or otherwise, would work
     * only as long as nothing realizes `integrationTests` during configuration;
     * once something did, the lookup would find no test task yet and the ordering
     * would vanish with no error.
     *
     * The collections select the task named `test`, and not every `Test` task:
     * `registerTestTasks()` of the shared build configuration adds `fastTest` and
     * `slowTest`, which partition the same suite by the `slow` tag, so depending on
     * all of them would run every test twice.
     */
    dependsOn(subprojects.map { subproject ->
        subproject.tasks.named { it == "test" }
    })
    dependsOn(localPublish)
    doLast {
        val f = file("$directory/_out/error-out.txt")
        project.logger.error(f.readText())
    }
}

tasks.register("buildAll") {
    dependsOn(integrationTests)
}

val check = tasks.named("check") {
    dependsOn(integrationTests)
}

apply(from = "version.gradle.kts")
val coreJvmCompilerVersion = extra["coreJvmCompilerVersion"] as String

val prepareBuildPerformanceSettings = tasks.register<Exec>("prepareBuildPerformanceSettings") {
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

    task("clean", "build")
}
