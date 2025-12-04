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
import groovy.util.NodeList
import io.spine.dependency.build.Ksp
import io.spine.dependency.lib.Protobuf
import io.spine.dependency.local.Compiler
import io.spine.dependency.local.TestLib
import io.spine.dependency.local.ToolBase
import io.spine.gradle.isSnapshot
import io.spine.gradle.publish.SpinePublishing
import io.spine.gradle.report.license.LicenseReporter
import java.util.jar.JarFile

plugins {
    `maven-publish`
    id("com.gradleup.shadow")
    `plugin-publish`
    `write-manifest`
}
LicenseReporter.generateReportIn(project)

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
                val compilerApi = Node(dependencies, "dependency")
                compilerApi.let {
                    Node(it, "groupId", "io.spine.tools")
                    Node(it, "artifactId", "compiler-api")
                    Node(it, "version", Compiler.version)
                    Node(it, "scope", "runtime")
                }

                Node(compilerApi, "exclusions").let {
                    excludeGroup(it, "org.jetbrains.kotlin")
                    excludeGroup(it, "com.google.protobuf")
                    excludeGroup(it, "io.spine.tools")
                }

                /*
                 * Add the dependency onto `io.spine.tools:compiler-jvm`,
                 * due to the same reasons as stated above.
                 *
                 * This dependency is required, in particular, to access
                 * the Proto definitions used by CoreJvm Gradle plugin extension
                 * via `CoreJvmOptions`.
                 *
                 * The appended code in `pom.xml` would look like this:
                 * ```
                 * <dependency>
                 *     <groupId>io.spine.tools</groupId>
                 *     <artifactId>compiler-jvm</artifactId>
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
                val compilerJvm = Node(dependencies, "dependency")
                compilerJvm.let {
                    Node(it, "groupId", "io.spine.tools")
                    Node(it, "artifactId", "compiler-jvm")
                    Node(it, "version", Compiler.version)
                    Node(it, "scope", "runtime")
                }

                Node(compilerJvm, "exclusions").let {
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
                 * Add the dependency on Protobuf Java library so that we can add it
                 * from our code. The code in `pom.xml` would look like this:
                 * ```
                 * <dependency>
                 *     <groupId>com.google.protobuf</groupId>
                 *     <artifactId>protobuf-java</artifactId>
                 *     <version>${Protobuf.version}</version>
                 *     <scope>runtime</scope>
                 * </dependency>
                 * ```
                 */
                Node(dependencies, "dependency").let {
                    Node(it, "groupId", "com.google.protobuf")
                    Node(it, "artifactId", "protobuf-java")
                    Node(it, "version", Protobuf.version)
                    Node(it, "scope", "runtime")
                }

                /*
                 * Add the dependency on Protobuf Kotlin library so that we can add it
                 * from our code. The code in `pom.xml` would look like this:
                 * ```
                 * <dependency>
                 *     <groupId>com.google.protobuf</groupId>
                 *     <artifactId>protobuf-kotlin</artifactId>
                 *     <version>${Protobuf.version}</version>
                 *     <scope>runtime</scope>
                 * </dependency>
                 * ```
                 */
                Node(dependencies, "dependency").let {
                    Node(it, "groupId", "com.google.protobuf")
                    Node(it, "artifactId", "protobuf-kotlin")
                    Node(it, "version", Protobuf.version)
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
         * Protobuf runtime and Gradle plugin will be available in the classpath because
         * fat JAR has the Maven `runtime` dependency on it.
         * Please see manipulations with `pom.xml` in the `publishing` block above.
         */
        "com/google/protobuf/**",
        "META-INF/gradle-plugins/com.google.protobuf.properties",

        /*
         * Excluding these types to avoid clashes at user's build classpath.
         *
         * The Compiler Gradle plugin will be added to the user's build via a dependency.
         * See the `pom.xml` manipulations above.
         */
        "io/spine/tools/compiler/**",
        "spine/compiler/**", // Protobuf definitions
        "META-INF/gradle-plugins/io.spine.compiler.properties", // Plugin declaration

        // Strip ArtifactMeta for:
        "META-INF/io.spine/io.spine.tools_compiler-gradle-plugin.meta", // Compiler Gradle Plugin
        "META-INF/io.spine/io.spine.tools_protobuf-setup-plugins.meta", // Protobuf Setup Plugins

        /*
         * Exclude Gradle types to reduce the size of the resulting JAR.
         *
         * Those required for the plugins are available at runtime anyway.
         */
        "org/gradle/**",

        // These types should be available at runtime via the Kotlin compiler.
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

tasks.jar {
    // There is no production source code in this module.
    // This module is for publishing only.
    enabled = false
}

disableDocumentationTasks()

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

/**
 * Make the `PluginMarkerMaven` publication refer to the fat JAR produced
 * the `plugin-bundle` module.
 *
 * The publication will still refer to the `plubin-bundle` artifact which
 * should be probably removed eventually.
 */
afterEvaluate {
    publishing {
        publications.withType<MavenPublication>().configureEach {
            if (name.endsWith("PluginMarkerMaven")) {
                pom.withXml {
                    val dependencies = dependenciesNode()
                    val dependency = Node(dependencies, "dependency")
                    dependency.let {
                        Node(it, "groupId", "io.spine.tools")
                        Node(it, "artifactId", "core-jvm-plugins")
                        Node(it, "version", project.version)
                        Node(it, "scope", "runtime")
                    }
                }
            }
        }
    }
}

/**
 * Finds or creates the `dependencies` node at the project node.
 */
private fun XmlProvider.dependenciesNode(): Node {
    val nodeName = "dependencies"
    val projectNode = asNode()
    val dependencies = (projectNode.get(nodeName) as? NodeList)
        ?.firstOrNull() as? Node
        ?: projectNode.appendNode(nodeName)
    return dependencies
}
