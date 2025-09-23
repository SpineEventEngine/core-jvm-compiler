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

@file:Suppress("RemoveRedundantQualifierName")

import groovy.util.Node
import io.spine.dependency.local.ProtoData
import io.spine.dependency.local.ToolBase
import io.spine.gradle.publish.SpinePublishing

buildscript {
    configurations {
        all {
            resolutionStrategy {
                dependencySubstitution {
                    substitute(module("io.spine.tools:spine-tool-base")).using(module(
                        io.spine.dependency.local.ToolBase.lib))
                }
                force(
                    io.spine.dependency.local.ToolBase.lib,
                )
            }
        }
    }
}

plugins {
    `maven-publish`
    id("com.github.johnrengelman.shadow")
}

/** The publishing settings from the root project. */
val spinePublishing = rootProject.the<SpinePublishing>()

/**
 * The ID of the far JAR artifact.
 *
 * This value is also used in `io.spine.tools.mc.java.gradle.Artifacts.kt`.
 */
val projectArtifact = spinePublishing.artifactPrefix + "plugins"

configurations {
    all {
        resolutionStrategy {
            dependencySubstitution {
                substitute(module("io.spine.tools:spine-tool-base")).using(module(ToolBase.lib))
                substitute(module("io.spine.tools:spine-psi-java")).using(module(ToolBase.psiJava))
            }
            force(
                ToolBase.lib,
            )
        }
    }
}

dependencies {
    implementation(project(":gradle-plugins"))
}

publishing {
    val groupName = project.group.toString()
    val versionName = project.version.toString()

    publications {
        create("fatJar", MavenPublication::class) {
            groupId = groupName
            artifactId = projectArtifact
            version = versionName
            artifact(tasks.shadowJar)

            /**
             * Manually add the dependency onto `io.spine:protodata`,
             * as there is no good way to remove all the dependencies
             * from the fat JAR artifact, but leave just this one.
             *
             * This dependency is required in order to place the ProtoData plugin
             * onto the build classpath, so that `mc-java` routines
             * could apply it programmatically.
             *
             * The appended content should look like this:
             * ```
             *     <dependency>
             *         <groupId>io.spine</groupId>
             *         <artifactId>protodata</artifactId>
             *         <version>$protoDataVersion</version>
             *         <scope>runtime</scope>
             *         <exclusions>
             *              <exclusion>
             *                  <groupId>io.spine.protodata</groupId>
             *                  <artifactId>*</artifactId>
             *              </exclusion>
             *              <exclusion>
             *                  <groupId>org.jetbrains.kotlin</groupId>
             *                  <artifactId>*</artifactId>
             *              </exclusion>
             *              <exclusion>
             *                  <groupId>com.google.protobuf</groupId>
             *                  <artifactId>*</artifactId>
             *              </exclusion>
             *              <exclusion>
             *                  <groupId>io.spine.tools</groupId>
             *                  <artifactId>*</artifactId>
             *              </exclusion>
             *         </exclusions>
             *    </dependency>
             * ```
             */
            pom.withXml {
                val projectNode: Node = asNode()
                val dependencies = Node(projectNode, "dependencies")
                val dependency = Node(dependencies, "dependency")
                Node(dependency, "groupId", "io.spine")
                Node(dependency, "artifactId", "protodata")
                Node(dependency, "version", ProtoData.version)
                Node(dependency, "scope", "runtime")

                val exclusions = Node(dependency, "exclusions")
                excludeGroupId(exclusions, "org.jetbrains.kotlin")
                excludeGroupId(exclusions, "com.google.protobuf")
                excludeGroupId(exclusions, "io.spine.tools")
            }
        }
    }
}

/**
 * Declare dependency explicitly to address the Gradle warning.
 */
@Suppress("unused")
val publishFatJarPublicationToMavenLocal: Task by tasks.getting {
    dependsOn(tasks.jar)
    println("Task `${this.name}` now depends on `${tasks.jar.name}`.")
}

tasks.publish {
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    exclude(
        /**
         * Exclude Kotlin runtime because it will be provided by Gradle runtime.
         */
        "kotlin/**",
        "kotlinx/**",

        /**
         * The Protobuf Gradle Plugin will be available in the classpath because
         * McJava Gradle Plugin is applied after it.
         */
        "com/google/protobuf/gradle/**",

        /**
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

        "spine/protodata/**", // Protobuf definitions
        "META-INF/gradle-plugins/io.spine.protodata.properties",  // Plugin declaration

        /**
         * Exclude Gradle types to reduce the size of the resulting JAR.
         *
         * Those required for the plugins are available at runtime anyway.
         */
        "org/gradle/**",

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

    setZip64(true)  /* The archive has way too many items. So using the Zip64 mode. */
    archiveClassifier.set("all")    /** To prevent Gradle setting something like `osx-x86_64`. */
    mergeServiceFiles("desc.ref")
    mergeServiceFiles("META-INF/services/io.spine.option.OptionsProvider")
}

fun excludeGroupId(exclusions: Node, groupId: String) {
    val exclusion = Node(exclusions, "exclusion")
    Node(exclusion, "groupId", groupId)
    Node(exclusion, "artifactId", "*")
}
