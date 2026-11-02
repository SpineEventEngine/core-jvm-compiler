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

@file:Suppress("AvoidApplyPluginMethod") // We apply some plugins via classes, not by id.

import com.google.common.io.Files
import io.spine.dependency.boms.BomsPlugin
import io.spine.dependency.build.CheckerFramework
import io.spine.dependency.build.ErrorProne
import io.spine.dependency.build.FindBugs
import io.spine.dependency.build.Ksp
import io.spine.dependency.lib.Caffeine
import io.spine.dependency.lib.Grpc
import io.spine.dependency.lib.Guava
import io.spine.dependency.lib.Jackson
import io.spine.dependency.lib.JacksonV2
import io.spine.dependency.lib.Kotlin
import io.spine.dependency.lib.KotlinPoet
import io.spine.dependency.lib.Protobuf
import io.spine.dependency.local.Base
import io.spine.dependency.local.Compiler
import io.spine.dependency.local.CoreJvm
import io.spine.dependency.local.Logging
import io.spine.dependency.local.Reflect
import io.spine.dependency.local.TestLib
import io.spine.dependency.local.Time
import io.spine.dependency.local.ToolBase
import io.spine.dependency.local.Validation
import io.spine.dependency.test.JUnit
import io.spine.gradle.checkstyle.CheckStyleConfig
import io.spine.gradle.github.pages.publishApiDocs
import io.spine.gradle.javac.configureErrorProne
import io.spine.gradle.javac.configureJavac
import io.spine.gradle.javadoc.JavadocConfig
import io.spine.gradle.kotlin.setFreeCompilerArgs
import io.spine.gradle.publish.IncrementGuard
import io.spine.gradle.report.license.LicenseReporter
import java.util.*

plugins {
    java
    `java-library`
    `java-test-fixtures`
    kotlin("jvm")
    id("write-manifest")
    id("com.google.protobuf")
    id("module-testing")
    id("dokka-setup")
    id("net.ltgt.errorprone")
    id("pmd-settings")
    `maven-publish`
    id("detekt-code-analysis")
    id("org.jetbrains.kotlinx.kover")
    id("project-report")
    idea
}
apply<BomsPlugin>()
apply<IncrementGuard>()

LicenseReporter.generateReportIn(project)
JavadocConfig.applyTo(project)
CheckStyleConfig.applyTo(project)

project.run {
    addDependencies()
    forceConfigurations()

    val javaVersion = BuildSettings.javaVersion
    configureJava(javaVersion)
    configureKotlin()
    setupTests()

    val generatedDir = "$projectDir/generated"
    val generatedResources = "$generatedDir/main/resources"
    prepareProtocConfigVersionsTask(generatedResources)
    setupSourceSets(generatedResources)

    configureTaskDependencies()
    setupDocPublishing()
}

typealias Module = Project

fun Module.addDependencies() {
    dependencies {
        errorprone(ErrorProne.core)

        compileOnlyApi(FindBugs.annotations)
        compileOnlyApi(CheckerFramework.annotations)
        ErrorProne.annotations.forEach { compileOnlyApi(it) }

        implementation(Guava.lib)
        implementation(Logging.lib)
    }
}

fun Module.forceConfigurations() {
    configurations {
        forceVersions()
        excludeProtobufLite()
        all {
            // Exclude outdated module.
            exclude(group = "io.spine", module = "spine-logging-backend")

            // Exclude in favor of `spine-validation-jvm-runtime`.
            exclude("io.spine", "spine-validate")
            resolutionStrategy {
                dependencySubstitution {
                    // Substitute the legacy coordinates with the current ones.
                    substitute(module("io.spine.tools:spine-plugin-base"))
                        .using(module(ToolBase.pluginBase))
                }

                val rs = this@resolutionStrategy
                val cfg = this@all
                Grpc.forceArtifacts(project, cfg, rs)
                Ksp.forceArtifacts(project, cfg, rs)

                Jackson.forceArtifacts(project, cfg, rs)
                Jackson.DataFormat.forceArtifacts(project, cfg, rs)
                Jackson.DataType.forceArtifacts(project, cfg, rs)
                JacksonV2.Core.forceArtifacts(project, cfg, rs)
                JacksonV2.DataFormat.forceArtifacts(project, cfg, rs)
                JacksonV2.DataType.forceArtifacts(project, cfg, rs)
                JacksonV2.Module.forceArtifacts(project, cfg, rs)
                // Jackson 2.x artifacts that only the IntelliJ Platform brings,
                // yet which still clash with the versions we resolve.
                JacksonV2.Junior.forceArtifacts(project, cfg, rs)

                force(
                    Grpc.bom,
                    Jackson.bom,
                    Jackson.annotations,
                    "commons-collections:commons-collections:3.2.2",
                    "commons-io:commons-io:2.22.0",
                    "org.objenesis:objenesis:3.6",
                    "org.bouncycastle:bcprov-jdk15on:1.70",
                    "io.github.classgraph:classgraph:4.8.184",
                    JUnit.bom,
                    Kotlin.bom,
                    Kotlin.Compiler.embeddable,
                    Kotlin.scriptRuntime,
                    Kotlin.GradlePlugin.api,
                    KotlinPoet.ksp,
                    KotlinPoet.lib,
                    Protobuf.compiler,
                    Caffeine.lib,
                    Reflect.lib,
                    Base.annotations,
                    Base.lib,
                    Base.environment,
                    Base.format,
                    Time.lib,
                    Time.javaExtensions,
                    Compiler.api,
                    Compiler.backend,
                    Compiler.params,
                    Compiler.gradleApi,
                    Compiler.pluginLib,
                    CoreJvm.core,
                    CoreJvm.client,
                    CoreJvm.server,
                    TestLib.lib,
                    ToolBase.fs,
                    ToolBase.code,
                    ToolBase.javaCode,
                    ToolBase.kotlinCode,
                    ToolBase.protoCode,
                    ToolBase.classicCodegen,
                    ToolBase.pluginBase,
                    ToolBase.jvmTools,
                    ToolBase.gradlePluginApi,
                    ToolBase.intellijPlatform,
                    ToolBase.intellijPlatformJava,
                    ToolBase.protobufSetupPlugins,
                    ToolBase.psiJava,
                    Logging.lib,
                    Logging.libJvm,
                    Logging.testLib,
                    Logging.grpcContext,
                    Compiler.api,
                    Compiler.gradleApi,
                    Compiler.jvm,
                    Validation.javaBundle,
                    Validation.runtime,
                )
            }
        }
    }
}

fun Module.configureJava(javaVersion: JavaLanguageVersion) {
    tasks.withType<JavaCompile>().configureEach {
        val javaVer = javaVersion.toString()
        sourceCompatibility = javaVer
        targetCompatibility = javaVer
        configureJavac()
        configureErrorProne()
    }
}

fun Module.configureKotlin() {
    kotlin {
        explicitApi()
        compilerOptions {
            jvmTarget.set(BuildSettings.jvmTarget)
            setFreeCompilerArgs()
        }
    }
}

fun Module.setupTests() {
    tasks {
        withType<Test> {
            // See https://github.com/gradle/gradle/issues/18647.
            jvmArgs(
                "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                "--add-opens", "java.base/java.util=ALL-UNNAMED",
                // Entries required for ErrorProne.
                "--add-opens", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                "--add-opens", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
                "--add-opens", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
                "--add-opens", "jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
                "--add-opens", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
                "--add-opens", "jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
                "--add-opens", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
                "--add-opens", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED"
            )
        }
    }
}

fun Module.prepareProtocConfigVersionsTask(generatedResources: String) {
    val prepareProtocConfigVersions = tasks.register("prepareProtocConfigVersions") {
        description = "Prepares the versions.properties file."

        val propertiesFile = file("$generatedResources/versions.properties")
        outputs.file(propertiesFile)

        val versions = Properties().apply {
            setProperty("baseVersion", Base.version)
            setProperty("protobufVersion", Protobuf.version)
            setProperty("gRPCVersion", Grpc.version)
        }

        @Suppress("UNCHECKED_CAST")
        inputs.properties(HashMap(versions) as MutableMap<String, *>)

        doLast {
            Files.createParentDirs(propertiesFile)
            propertiesFile.createNewFile()
            propertiesFile.outputStream().use {
                versions.store(it,
                    "Versions of dependencies of the Spine Model Compiler for Java plugin and" +
                            " the Spine Protoc plugin.")
            }
        }
    }

    tasks.processResources {
        dependsOn(prepareProtocConfigVersions)
    }
}

fun Module.setupSourceSets(generatedResources: String) {
    sourceSets.main {
        resources.srcDir(generatedResources)
    }
}

/**
 * Configures documentation publishing for this subproject.
 */
fun Module.setupDocPublishing() {
    publishApiDocs {
        rootFolder.set(rootDir)
    }
}
