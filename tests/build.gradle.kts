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

import io.spine.dependency.build.ErrorProne
import io.spine.dependency.lib.Grpc
import io.spine.dependency.lib.Kotlin
import io.spine.dependency.lib.KotlinPoet
import io.spine.dependency.boms.BomsPlugin
import io.spine.dependency.local.Base
import io.spine.dependency.local.CoreJava
import io.spine.dependency.local.Logging
import io.spine.dependency.local.ProtoData
import io.spine.dependency.local.Reflect
import io.spine.dependency.local.TestLib
import io.spine.dependency.local.Time
import io.spine.dependency.local.ToolBase
import io.spine.dependency.local.Validation
import io.spine.dependency.test.JUnit
import io.spine.dependency.test.Truth
import io.spine.gradle.javac.configureErrorProne
import io.spine.gradle.javac.configureJavac
import io.spine.gradle.kotlin.setFreeCompilerArgs
import io.spine.gradle.publish.PublishingRepos.gitHub
import io.spine.gradle.repo.standardToSpineSdk
import io.spine.gradle.testing.configureLogging

buildscript {

    val baseRoot = "${rootDir}/.."
    val versionGradle = "${baseRoot}/version.gradle.kts"
    apply(from = versionGradle)

    standardSpineSdkRepositories()

    val mcJavaVersion: String by extra
    val protoData = io.spine.dependency.local.ProtoData
    dependencies {
        classpath(io.spine.dependency.lib.Guava.lib)
        classpath(io.spine.dependency.lib.Protobuf.GradlePlugin.lib) {
            exclude(group = "com.google.guava")
        }
        classpath(io.spine.dependency.build.ErrorProne.GradlePlugin.lib) {
            exclude(group = "com.google.guava")
        }
        classpath(protoData.pluginLib)
        classpath(io.spine.dependency.local.McJava.pluginLib(mcJavaVersion))
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
                force(
                    "io.spine:protodata:${protoData.version}",
                    io.spine.dependency.local.Reflect.lib,
                    io.spine.dependency.local.Base.lib,
                    io.spine.dependency.local.Time.lib,
                    toolBase.lib,
                    toolBase.pluginBase,
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
        standardToSpineSdk()
        gitHub("base")
        gitHub("tool-base")
        gitHub("model-compiler")
        mavenLocal()
    }

    group = "io.spine.tools.tests"
    version = extra["versionToPublish"]!!

    configurations {
        forceVersions()
        excludeProtobufLite()
        all {
            resolutionStrategy {
                force(
                    KotlinPoet.lib,
                    Reflect.lib,
                    Base.lib,
                    CoreJava.server,
                    Time.lib,
                    TestLib.lib,
                    ToolBase.lib,
                    ToolBase.pluginBase,
                    Logging.lib,
                    Logging.libJvm,
                    Logging.middleware,
                    ToolBase.psiJava,
                    ProtoData.api,
                    Validation.config,
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
        plugin("io.spine.mc-java")
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
        errorproneJavac(ErrorProne.javacPlugin)
        ErrorProne.annotations.forEach { compileOnly(it) }
        implementation(Base.lib)
        implementation(Logging.lib)
    }

    with(configurations) {
        doForceVersions(this)
        all {
            resolutionStrategy {
                force(
                    Validation.runtime,
                )
            }
        }
    }

    disableDocumentationTasks()
}
