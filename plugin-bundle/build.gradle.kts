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
import io.spine.dependency.lib.Protobuf
import io.spine.dependency.local.Compiler
import io.spine.dependency.local.TestLib
import io.spine.dependency.local.ToolBase
import io.spine.gradle.publish.SpinePublishing

plugins {
    `maven-publish`
    id("com.gradleup.shadow")
}

/** The publishing settings from the root project. */
val spinePublishing = rootProject.the<SpinePublishing>()

/**
 * The ID of the far JAR artifact.
 *
 * This value is also used in `io.spine.tools.mc.java.gradle.Artifacts.kt`.
 */
val projectArtifact = spinePublishing.artifactPrefix + "plugins"

dependencies {
    implementation(project(":gradle-plugins"))

    arrayOf(
        gradleTestKit(),
        project(":base") /* Open the `DependencyHolder` class to tests. */,
        TestLib.lib,
        ToolBase.jvmTools,
        ToolBase.pluginTestlib,
    ).forEach {
        testImplementation(it)
    }
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
             * Manually add the dependency onto `io.spine.tools:compiler-api`,
             * as there is no good way to remove all the dependencies
             * from the fat JAR artifact, but leave just this one.
             *
             * This dependency is required in order to place the Spine Compiler API
             * onto the build classpath, so that `core-jvm` routines
             * could apply it programmatically.
             *
             * The appended content should look like this:
             * ```
             *     <dependency>
             *         <groupId>io.spine.tools</groupId>
             *         <artifactId>compiler-api</artifactId>
             *         <version>$compilerVersion</version>
             *         <scope>runtime</scope>
             *         <exclusions>
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
             *     <dependency>
             *         <groupId>com.google.protobuf</groupId>
             *         <artifactId>protobuf-gradle-plugin</artifactId>
             *         <version>${Protobuf.GradlePlugin.version}</version>
             *         <scope>runtime</scope>
             *    </dependency>
             * ```
             */
            pom.withXml {
                val projectNode: Node = asNode()
                val dependencies = Node(projectNode, "dependencies")
                val dependency = Node(dependencies, "dependency")
                Node(dependency, "groupId", "io.spine.tools")
                Node(dependency, "artifactId", "compiler-api")
                Node(dependency, "version", Compiler.version)
                Node(dependency, "scope", "runtime")

                val exclusions = Node(dependency, "exclusions")
                excludeGroupId(exclusions, "org.jetbrains.kotlin")
                excludeGroupId(exclusions, "com.google.protobuf")
                excludeGroupId(exclusions, "io.spine.tools")

                val protoDependency = Node(dependencies, "dependency")
                Node(protoDependency, "groupId", "com.google.protobuf")
                Node(protoDependency, "artifactId", "protobuf-gradle-plugin")
                Node(protoDependency, "version", Protobuf.GradlePlugin.version)
                Node(protoDependency, "scope", "runtime")
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
         * CoreJvm Gradle Plugin is applied after it.
         */
        "com/google/protobuf/gradle/**",
        "META-INF/gradle-plugins/com.google.protobuf.properties",

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

/**
 * Tests use the artifacts published to `mavenLocal`, so we need to publish them all first.
 */
tasks.test {
    dependsOn(rootProject.tasks.named("localPublish"))
}
