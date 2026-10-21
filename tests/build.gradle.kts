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

import io.spine.dependency.build.ErrorProne
import io.spine.dependency.lib.Caffeine
import io.spine.dependency.lib.Grpc
import io.spine.dependency.lib.Jackson
import io.spine.dependency.lib.JacksonV2
import io.spine.dependency.lib.Kotlin
import io.spine.dependency.lib.KotlinPoet
import io.spine.dependency.boms.BomsPlugin
import io.spine.dependency.isDokka
import io.spine.dependency.local.Base
import io.spine.dependency.local.CoreJvm
import io.spine.dependency.local.CoreJvmCompiler
import io.spine.dependency.local.Logging
import io.spine.dependency.local.Compiler
import io.spine.dependency.local.Reflect
import io.spine.dependency.local.TestLib
import io.spine.dependency.local.Time
import io.spine.dependency.local.ToolBase
import io.spine.dependency.local.Validation
import io.spine.gradle.javac.configureErrorProne
import io.spine.gradle.javac.configureJavac
import io.spine.gradle.kotlin.setFreeCompilerArgs
import io.spine.gradle.publish.PublishingRepos.gitHub
import io.spine.gradle.repo.standardToSpineSdk

buildscript {

    val baseRoot = "${rootDir}/.."
    val versionGradle = "${baseRoot}/version.gradle.kts"
    apply(from = versionGradle)

    /*
     * Maven Local comes first, restricted to the two artifacts of the root build that
     * this build consumes, so that the tests run against what `localPublish` has just
     * installed. `standardToSpineSdk()`, which `standardSpineSdkRepositories()` applies,
     * places `mavenLocal()` last — so once these coordinates have been published to a
     * remote repository, that repository would serve them and this build would silently
     * test the artifacts published earlier.
     *
     * The filter deliberately fails open: a coordinate missing from Maven Local still
     * resolves from the remote repositories, rather than failing the build of someone
     * who has not run `localPublish` yet.
     */
    repositories {
        val coreJvmCompiler = io.spine.dependency.local.CoreJvmCompiler
        mavenLocal {
            content {
                includeModule(coreJvmCompiler.group, coreJvmCompiler.gradlePluginArtifact)
                includeModule(coreJvmCompiler.group, coreJvmCompiler.compilerPluginsArtifact)
            }
        }
    }
    standardSpineSdkRepositories()

    val coreJvmCompilerVersion: String by extra
    val compiler = io.spine.dependency.local.Compiler
    dependencies {
        classpath(io.spine.dependency.lib.Guava.lib)
        classpath(io.spine.dependency.lib.Protobuf.GradlePlugin.lib) {
            exclude(group = "com.google.guava")
        }
        classpath(io.spine.dependency.build.ErrorProne.GradlePlugin.lib) {
            exclude(group = "com.google.guava")
        }
        // The `CoreJvmCompiler` object of `config` does not know this artifact yet.
        classpath("io.spine.tools:core-jvm-gradle-plugin:$coreJvmCompilerVersion")
        classpath(enforcedPlatform(io.spine.dependency.kotlinx.Coroutines.bom))
        classpath(enforcedPlatform(io.spine.dependency.lib.Grpc.bom))
    }

    with(configurations) {
        doForceVersions(this)
        val toolBase = io.spine.dependency.local.ToolBase
        val logging = io.spine.dependency.local.Logging
        all {
            resolutionStrategy {
                val cfg = this@all
                val rs = this@resolutionStrategy
                io.spine.dependency.lib.Kotlin.StdLib.forceArtifacts(project, cfg, rs)
                io.spine.dependency.lib.JacksonV2.Core.forceArtifacts(project, cfg, rs)
                io.spine.dependency.lib.JacksonV2.DataFormat.forceArtifacts(project, cfg, rs)
                io.spine.dependency.lib.JacksonV2.DataType.forceArtifacts(project, cfg, rs)
                io.spine.dependency.lib.JacksonV2.Module.forceArtifacts(project, cfg, rs)
                // Jackson 2.x artifacts that only the IntelliJ Platform brings,
                // yet which still clash with the versions we resolve.
                io.spine.dependency.lib.JacksonV2.Junior.forceArtifacts(project, cfg, rs)
                force(
                    io.spine.dependency.lib.Jackson.annotations,
                    io.spine.dependency.lib.Jackson.bom,
                    io.spine.dependency.lib.Kotlin.bom,
                    io.spine.dependency.local.Reflect.lib,
                    io.spine.dependency.local.Base.annotations,
                    io.spine.dependency.local.Base.lib,
                    io.spine.dependency.local.Base.environment,
                    io.spine.dependency.local.Base.format,
                    io.spine.dependency.local.Time.lib,
                    io.spine.dependency.local.Time.javaExtensions,
                    toolBase.fs,
                    toolBase.code,
                    toolBase.javaCode,
                    toolBase.kotlinCode,
                    toolBase.protoCode,
                    toolBase.classicCodegen,
                    toolBase.pluginBase,
                    toolBase.jvmTools,
                    logging.lib,
                    logging.libJvm,
                    logging.middleware,
                    io.spine.dependency.local.Validation.runtime,
                )
            }
        }
    }
}

@Suppress("RemoveRedundantQualifierName") // Cannot use imports here.
plugins {
    java
    kotlin("jvm")
    idea
    id("com.google.protobuf")
    id("net.ltgt.errorprone")
}

val baseRoot = "$rootDir/.."

allprojects {
    apply(from = "$baseRoot/version.gradle.kts")
    apply(plugin = "java")

    repositories {
        // See the comment in the `buildscript` block above.
        mavenLocal {
            content {
                includeModule(CoreJvmCompiler.group, CoreJvmCompiler.gradlePluginArtifact)
                includeModule(CoreJvmCompiler.group, CoreJvmCompiler.compilerPluginsArtifact)
            }
        }
        standardToSpineSdk()
        gitHub("base")
        gitHub("tool-base")
        gitHub("core-jvm-compiler")
        mavenLocal()
    }

    configurations.all {
        if (isDokka) {
            return@all
        }
        resolutionStrategy {
            // Floor artifacts request the pre-refresh versions of these,
            // tripping `failOnVersionConflict()`; the Protobuf runtime must
            // additionally never be older than the refreshed gencode.
            force(
                io.spine.dependency.lib.Protobuf.javaLib,
                io.spine.dependency.kotlinx.Coroutines.bom,
                io.spine.dependency.kotlinx.AtomicFu.lib,
            )
        }
    }

    group = "io.spine.tools.tests"
    version = extra["versionToPublish"]!!

    configurations {
        forceVersions()
        excludeProtobufLite()
        all {
            resolutionStrategy {
                Grpc.forceArtifacts(project, this@all, this@resolutionStrategy)
                Kotlin.StdLib.forceArtifacts(project, this@all, this@resolutionStrategy)
                force(
                    // Jackson 2.x artifacts that only the IntelliJ Platform brings,
                    // yet which still clash with the versions we resolve.
                    JacksonV2.Junior.artifact(JacksonV2.Junior.objects),
                    JacksonV2.Module.artifact(JacksonV2.Module.kotlin),
                    Base.annotations,
                    Base.lib,
                    Base.environment,
                    Base.format,
                    Caffeine.lib,
                    Compiler.api,
                    CoreJvm.server,
                    Grpc.bom,
                    Jackson.bom,
                    Kotlin.bom,
                    KotlinPoet.lib,
                    Logging.lib,
                    Logging.libJvm,
                    Reflect.lib,
                    TestLib.lib,
                    Time.javaExtensions,
                    Time.lib,
                    ToolBase.classicCodegen,
                    ToolBase.code,
                    ToolBase.fs,
                    ToolBase.intellijPlatform,
                    ToolBase.intellijPlatformJava,
                    ToolBase.javaCode,
                    ToolBase.kotlinCode,
                    ToolBase.pluginBase,
                    ToolBase.protoCode,
                    ToolBase.psiJava,
                    Validation.javaBundle,
                    Validation.runtime,
                )
            }

            // Exclude this stale artifact from all transitive dependencies.
            exclude("io.spine", "spine-validate")
        }
    }
    disableDocumentationTasks()
}

subprojects {

    apply {
        plugin("com.google.protobuf")
        plugin("kotlin")
        plugin("module-testing")
        plugin("io.spine.core-jvm")
        plugin("net.ltgt.errorprone")
        plugin("idea")
    }
    apply<BomsPlugin>()

    java {
        sourceCompatibility = BuildSettings.javaVersionCompat
        targetCompatibility = BuildSettings.javaVersionCompat

        tasks.withType<JavaCompile>().configureEach {
            configureJavac()
            configureErrorProne()
        }
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(BuildSettings.jvmTarget)
            setFreeCompilerArgs()
        }
    }

    dependencies {
        errorprone(ErrorProne.core)
        ErrorProne.annotations.forEach { compileOnly(it) }
        implementation(Base.lib)
        implementation(Logging.lib)
    }

    with(configurations) {
        doForceVersions(this)
        all {
            resolutionStrategy {
                JacksonV2.Core.forceArtifacts(project, this@all, this@resolutionStrategy)
                JacksonV2.DataFormat.forceArtifacts(project, this@all, this@resolutionStrategy)
                JacksonV2.DataType.forceArtifacts(project, this@all, this@resolutionStrategy)
                JacksonV2.Module.forceArtifacts(project, this@all, this@resolutionStrategy)
                // Jackson 2.x artifacts that only the IntelliJ Platform brings,
                // yet which still clash with the versions we resolve.
                JacksonV2.Junior.forceArtifacts(project, this@all, this@resolutionStrategy)
                force(
                    Jackson.annotations,
                    ToolBase.gradlePluginApi,
                    ToolBase.jvmTools,
                    ToolBase.protobufSetupPlugins,
                    Validation.runtime,
                )
            }
        }
    }

    disableDocumentationTasks()
}
