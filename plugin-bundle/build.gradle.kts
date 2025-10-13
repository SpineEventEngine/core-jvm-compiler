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

import groovy.util.Node
import io.spine.dependency.build.Ksp
import io.spine.dependency.lib.Protobuf
import io.spine.dependency.local.Compiler
import io.spine.dependency.local.TestLib
import io.spine.dependency.local.ToolBase
import io.spine.gradle.isSnapshot
import io.spine.gradle.publish.SpinePublishing
import java.util.jar.JarFile

plugins {
    `maven-publish`
    id("com.gradleup.shadow")
    `plugin-publish`
    `write-manifest`
}

/** The publishing settings from the root project. */
val spinePublishing = rootProject.the<SpinePublishing>()

/**
 * The ID of the far JAR artifact.
 *
 * This value is also used in `io.spine.tools.mc.java.gradle.Artifacts.kt`.
 */
val projectArtifact = spinePublishing.artifactPrefix + "plugins"

// Resolvable configurations to obtain IntelliJ Platform artifacts
// without bringing them as runtime deps.
val intellijPlatform: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

val intellijPlatformJava: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

dependencies {
    implementation(project(":gradle-plugins"))

    arrayOf(
        gradleTestKit(),
        project(":base"), /* Open dependency objects to tests. */
        TestLib.lib,
        ToolBase.jvmTools,
        ToolBase.pluginTestlib,
    ).forEach {
        testImplementation(it)
    }

    // Add IntelliJ Platform artifacts to dedicated resolvable configurations.
    add(intellijPlatform.name, ToolBase.intellijPlatform)
    add(intellijPlatformJava.name, ToolBase.intellijPlatformJava)
}

@Suppress("unused")
afterEvaluate {
    // This module does not have source code.
    val sourcesJar: Task by tasks.getting {
        enabled = false
    }

    // This module does not have source code.
    val javadocJar: Task by tasks.getting {
        enabled = false
    }

    val testFixturesJar by tasks.getting {
        enabled = false
    }
}

publishing {
    publications {
        clear()
        create("maven", MavenPublication::class) {
            groupId = project.group.toString()
            artifactId = projectArtifact
            version = project.version.toString()
            artifact(tasks.shadowJar)

            pom.withXml {
                val projectNode: Node = asNode()
                val dependencies = Node(projectNode, "dependencies")
                /*
                 * Add the dependency onto `io.spine.tools:compiler-api`,
                 * as there is no good way to remove all the dependencies
                 * from the fat JAR artifact, but leave just this one.
                 *
                 * This dependency is required in order to place the Spine Compiler API
                 * onto the build classpath, so that `core-jvm` routines
                 * could apply it programmatically.
                 *
                 * The appended code in `pom.xml` would look like this:
                 * ```
                 * <dependency>
                 *     <groupId>io.spine.tools</groupId>
                 *     <artifactId>compiler-api</artifactId>
                 *     <version>${Compiler.version}</version>
                 *     <scope>runtime</scope>
                 *     <exclusions>
                 *          <exclusion>
                 *              <groupId>org.jetbrains.kotlin</groupId>
                 *              <artifactId>*</artifactId>
                 *          </exclusion>
                 *          <exclusion>
                 *              <groupId>com.google.protobuf</groupId>
                 *              <artifactId>*</artifactId>
                 *          </exclusion>
                 *          <exclusion>
                 *              <groupId>io.spine.tools</groupId>
                 *              <artifactId>*</artifactId>
                 *          </exclusion>
                 *     </exclusions>
                 * </dependency>
                 * ```
                 */
                val dependency = Node(dependencies, "dependency")
                dependency.let {
                    Node(it, "groupId", "io.spine.tools")
                    Node(it, "artifactId", "compiler-api")
                    Node(it, "version", Compiler.version)
                    Node(it, "scope", "runtime")
                }

                Node(dependency, "exclusions").let {
                    excludeGroup(it, "org.jetbrains.kotlin")
                    excludeGroup(it, "com.google.protobuf")
                    excludeGroup(it, "io.spine.tools")
                }

                /*
                 * Add the dependency on Protobuf Gradle Plugin so that we can add it
                 * from our code. The code in `pom.xml` would look like this:
                 * ```
                 * <dependency>
                 *     <groupId>com.google.protobuf</groupId>
                 *     <artifactId>protobuf-gradle-plugin</artifactId>
                 *     <version>${Protobuf.GradlePlugin.version}</version>
                 *     <scope>runtime</scope>
                 * </dependency>
                 * ```
                 */
                Node(dependencies, "dependency").let {
                    Node(it, "groupId", "com.google.protobuf")
                    Node(it, "artifactId", "protobuf-gradle-plugin")
                    Node(it, "version", Protobuf.GradlePlugin.version)
                    Node(it, "scope", "runtime")
                }
                /*
                 * Add the dependency on KSP Gradle Plugin as well.
                 * The expected XML output:
                 * ```
                 * <dependency>
                 *     <groupId>${Ksp.group}</groupId>
                 *     <artifactId>${Ksp.gradlePluginArtifactName}</artifactId>
                 *     <version>${Ksp.version}</version>
                 *     <scope>runtime</scope>
                 * </dependency>
                 * ```
                 */
                Node(dependencies, "dependency").let {
                    Node(it, "groupId", Ksp.group)
                    Node(it, "artifactId", Ksp.gradlePluginArtifactName)
                    Node(it, "version", Ksp.version)
                    Node(it, "scope", "runtime")
                }
            }
        }
    }
}

// As defined in `versions.gradle.kts`.
val versionToPublish: String by extra

// Do not publish to Gradle Plugin Portal snapshot versions.
// It is prohibited by their policy: https://plugins.gradle.org/docs/publish-plugin
val publishPlugins: Task by tasks.getting {
    enabled = !versionToPublish.isSnapshot()
}

tasks.publish {
    dependsOn(tasks.shadowJar)
    dependsOn(publishPlugins)
}

fun JarFile.entriesAsSet(): Set<String> = entries().asSequence().map { it.name }.toSet()

tasks.shadowJar {
    // Exclude files that are already provided by the IntelliJ Platform artifacts
    // from ToolBase so that we don't duplicate them in the fat JAR.

    var pathsToExclude = setOf<String>()

    // Resolve lazily at task execution time to avoid unnecessary resolution during configuration.
    doFirst {
        val ijPlatformJar = JarFile(intellijPlatform.files.single())
        val ijPlatformJavaJar = JarFile(intellijPlatformJava.files.single())

        val filesCombined =
            ijPlatformJar.use { pJar ->
                ijPlatformJavaJar.use { pjJar ->
                    pJar.entriesAsSet() + pjJar.entriesAsSet()
                }
            }

        // We still need Google Guava's types.
        pathsToExclude = filesCombined.filter {
            !(it.contains("com/google/common")
                    || it.contains("com/google/thirdparty"))
        }.toSet()
    }

    exclude {
        it.path in pathsToExclude
    }

    exclude(
        /*
         * Exclude Kotlin runtime because it will be provided by the Gradle runtime.
         */
        "kotlin/**",

        /*
         * Exclude Coroutines. They also will be present. The rest of `kotlinx` should stay.
         */
        "kotlinx/coroutines/**",

        /*
         * The Protobuf Gradle Plugin will be available in the classpath because
         * fat JAR has the Maven `runtime` dependency on it.
         * Please see manipulations with `pom.xml` below.
         */
        "com/google/protobuf/gradle/**",
        "META-INF/gradle-plugins/com.google.protobuf.properties",

        /*
         * Excluding these types to avoid clashes at user's build classpath.
         *
         * The ProtoData plugin will be added to the user's build via a dependency.
         * See the `pom.xml` manipulations above.
         */
        "io/spine/protodata/*",
        "io/spine/protodata/plugin/**",
        "io/spine/protodata/renderer/**",
        "io/spine/protodata/type/**",
        "io/spine/protodata/cli/app/**",
        "io/spine/protodata/gradle/plugin/**",
        "io/spine/protodata/java/*",
        "io/spine/protodata/java/annotation/**",
        "io/spine/protodata/java/file/**",
        "io/spine/protodata/protoc/**",

//       TODO: Uncomment these after the Spine Compiler is available from Gradle Plugin Portal.
//
//        "io/spine/tools/compiler/*",
//        "io/spine/tools/compiler/plugin/**",
//        "io/spine/tools/compiler/renderer/**",
//        "io/spine/tools/compiler/type/**",
//        "io/spine/tools/compiler/cli/app/**",
//        "io/spine/tools/compiler/gradle/plugin/**",
//        "io/spine/tools/compiler/jvm/*",
//        "io/spine/tools/compiler/jvm/annotation/**",
//        "io/spine/tools/compiler/jvm/file/**",
//        "io/spine/tools/compiler/protoc/**",
//        "spine/compiler/**",

        // TODO: Uncomment these as well once the Spine Compiler is ready.
        // Plugin declaration
        "META-INF/gradle-plugins/io.spine.protodata.properties",
//      "META-INF/gradle-plugins/io.spine.compiler.properties",


        // Protobuf definitions
        "spine/protodata/**",

        /**
         * Exclude Gradle types to reduce the size of the resulting JAR.
         *
         * Those required for the plugins are available at runtime anyway.
         */
        "org/gradle/**",

        // These types should be available at run-time via Kotlin compiler.
        "ksp/**",
        "com/google/devtools/ksp/**",

        // Do not declare third-party Gradle plugins,
        // especially those stripped above.
        "META-INF/gradle-plugins/com.google**",

        // Exclude license files that cause or may cause issues with LicenseReport.
        // We analyze these files when building artifacts we depend on.
        "about_files/**",
        "license/**",

        "ant_tasks/**", // `resource-ant.jar` is of no use here.

        /* Exclude `https://github.com/JetBrains/pty4j`.
          We don't need the terminal. */
        "resources/com/pty4j/**",

        // Protobuf files.
        "google/**",
        "spine/**",
        "src/**",

        // Java source code files of the package `org.osgi`.
        "OSGI-OPT/**",

        /*
          Exclude `https://github.com/JetBrains/pty4j`.
          We don't need the terminal.
         */
        "resources/com/pty4j/**",
    )

    /* The archive has way too many items. So use the Zip64 mode. */
    setZip64(true)

    /* Prevent Gradle setting something like `osx-x86_64`. */
    archiveClassifier.set("")

    duplicatesStrategy = DuplicatesStrategy.INCLUDE  // To allow further merging.
    append("desc.ref")
    append("META-INF/services/io.spine.option.OptionsProvider")
}

fun excludeGroup(exclusions: Node, groupId: String) {
    Node(exclusions, "exclusion").let {
        Node(it, "groupId", groupId)
        Node(it, "artifactId", "*")
    }
}

/**
 * Tests use the artifacts published to `mavenLocal`, so we need to publish them all first.
 */
tasks.test {
    dependsOn(rootProject.tasks.named("localPublish"))
}

gradlePlugin {
    website.set("https://spine.io/")
    vcsUrl.set("https://github.com/SpineEventEngine/core-jvm-compiler.git")
    plugins {
        val pluginTags = listOf(
            "ddd",
            "codegen",
            "java",
            "kotlin",
            "jvm"
        )

        create("coreJvmCompilerPlugins") {
            id = "io.spine.core-jvm"
            implementationClass = "io.spine.tools.core.jvm.gradle.plugins.CoreJvmPlugin"
            displayName = "Spine CoreJvm Compiler Plugins"
            description = "Compiles Protobuf files with custom options of CoreJvm Library"
            tags.set(pluginTags)
        }
    }
}
