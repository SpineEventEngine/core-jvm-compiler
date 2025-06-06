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

import io.spine.dependency.lib.Grpc
import io.spine.dependency.local.Spine
import io.spine.dependency.local.Base
import io.spine.gradle.repo.standardToSpineSdk
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.all
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.resolutionStrategy

buildscript {

    // NOTE: this file is copied from the root project in the test setup.
    apply(from = "$rootDir/test-env.gradle")
    apply(from = "${extra["enclosingRootDir"]}/version.gradle.kts")

    standardSpineSdkRepositories()

    val coreJvmCompilerVersion: String by extra
    dependencies {
        io.spine.dependency.lib.Protobuf.libs.forEach { classpath(it) }

        // Exclude `guava:18.0` as a transitive dependency by Protobuf Gradle plugin.
        classpath(io.spine.dependency.lib.Protobuf.GradlePlugin.lib) {
            exclude(group = "com.google.guava")
        }
        classpath(io.spine.dependency.local.CoreJvmCompiler.pluginLib(coreJvmCompilerVersion))
    }
}

plugins {
    java
}

allprojects {
    group = "io.spine.test"
    version = "3.14"
}

subprojects {

    apply(plugin = "java")
    apply(from = "$rootDir/test-env.gradle")

    val enclosingRootDir: String by extra
    apply {
        plugin("com.google.protobuf")
        plugin("io.spine.core-jvm")
        from("${enclosingRootDir}/version.gradle.kts")
    }

    repositories.standardToSpineSdk()

    configurations {
        all {
            resolutionStrategy {
                force(
                    Base.lib,
                )
            }
        }
    }

    tasks.findByName("launchProtoData")?.apply { this as JavaExec
        debugOptions {
            enabled.set(false) // Set this option to `true` to enable remote debugging.
            port.set(5566)
            server.set(true)
            suspend.set(true)
        }
    }

    dependencies {
        implementation(Base.lib)
        implementation(platform(Grpc.bom))
        implementation(Grpc.stub)
        implementation(Grpc.protobuf)
    }

    sourceSets {
        main {
            java.srcDirs("$projectDir/generated/main/java", "$projectDir/generated/main/spine")
            resources.srcDir("$projectDir/generated/main/resources")
            (extensions.getByName("proto") as SourceDirectorySet).srcDir("$projectDir/src/main/proto")
        }
    }
}
