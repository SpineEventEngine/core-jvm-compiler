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
import io.spine.dependency.lib.Kotlin
import io.spine.dependency.lib.Protobuf
import io.spine.dependency.local.Compiler
import io.spine.dependency.local.CoreJvmCompiler
import io.spine.dependency.local.TestLib
import io.spine.dependency.local.ToolBase
import io.spine.dependency.local.Validation
import io.spine.dependency.local.Spine
import io.spine.dependency.local.Time
import io.spine.gradle.isSnapshot
import io.spine.gradle.publish.setup
import io.spine.gradle.report.license.LicenseReporter
import java.util.jar.JarFile
import org.gradle.api.publish.maven.tasks.PublishToMavenLocal
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository

plugins {
    module
    id("io.spine.artifact-meta")
    `maven-publish`
    id("com.gradleup.shadow")
    `plugin-publish`
    `write-manifest`
}
LicenseReporter.generateReportIn(project)

/**
 * The ID used for publishing this module.
 */
val moduleArtifactId: String = CoreJvmCompiler.fatJarArtifact

artifactMeta {
    artifactId.set(moduleArtifactId)
    addDependencies(
        // Add Validation module dependencies that we use for project configuration
        // to which the CoreJvm Gradle Plugin is applied.
        Validation.javaBundle,
        Validation.runtime,

        // These dependencies are written for integration tests.
        Kotlin.GradlePlugin.lib,
        Protobuf.GradlePlugin.lib,
        Ksp.artifact(Ksp.gradlePlugin),
    )
    excludeConfigurations {
        containing(*buildToolConfigurations)
    }
}

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
    compileOnly(gradleKotlinDsl())
    implementation(Compiler.pluginLib)
    implementation(Compiler.params)
    implementation(ToolBase.jvmTools)
    implementation(Validation.gradlePluginLib)
    implementation(Time.gradlePlugin)

    compileOnly(Protobuf.GradlePlugin.lib)
        ?.because("We access the Protobuf Gradle Plugin extension.")

    // Module dependencies
    listOf(
        ":base",
        ":annotation",
        ":entity",
        ":grpc",
        ":signal",
        ":marker",
        ":message-group",
        ":uuid",
        ":comparable",
        ":routing"
    ).forEach {
        implementation(project(it))
    }

    arrayOf(
        gradleApi(),
        gradleKotlinDsl(),
        gradleTestKit(),
        project(":base"), /* Open dependency objects to tests. */
        TestLib.lib,
        Kotlin.GradlePlugin.lib,
        ToolBase.pluginTestlib,
        testFixtures(project(":base")),
    ).forEach {
        testImplementation(it)
    }

    // Add IntelliJ Platform artifacts to dedicated resolvable configurations.
    add(intellijPlatform.name, ToolBase.intellijPlatform)
    add(intellijPlatformJava.name, ToolBase.intellijPlatformJava)
}

publishing {
    publications {
        create("pluginBundle", MavenPublication::class) {
            // `groupId`, `artifactId` and `version` are filled in by `CustomPublicationHandler`.
            artifact(tasks.shadowJar)
            tuneDependencies()
        }
    }
}

private fun MavenPublication.tuneDependencies() {
    pom.withXml {
        val projectNode = asNode()
        val dependencies = Node(projectNode, "dependencies")
        fun dependencyNode() = Node(dependencies, "dependency")

        fun spineToolsGroup(parent: Node) = Node(parent, "groupId", Spine.toolsGroup)
        fun artifactId(parent: Node, value: String) = Node(parent, "artifactId", value)
        fun version(parent: Node, value: String) = Node(parent, "version", value)
        fun runtimeScope(parent: Node) = Node(parent, "scope", "runtime")
        fun addExclusions(parent: Node) {
            Node(parent, "exclusions").let {
                excludeGroup(it, "org.jetbrains.kotlin")
                excludeGroup(it, "com.google.protobuf")
                excludeGroup(it, "io.spine.tools")
            }
        }

        /*
         * Add the dependency onto `io.spine.tools:compiler-api`,
         * as there is no good way to remove all the dependencies
         * from the fat JAR artifact but leave just this one.
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
        val compilerApi = dependencyNode()
        compilerApi.let {
            spineToolsGroup(it)
            artifactId(it, "compiler-api")
            version(it, Compiler.version)
            runtimeScope(it)
        }
        addExclusions(compilerApi)

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
        val compilerJvm = dependencyNode()
        compilerJvm.let {
            spineToolsGroup(it)
            artifactId(it, "compiler-jvm")
            version(it, Compiler.version)
            runtimeScope(it)
        }
        addExclusions(compilerJvm)

        /*
         * Add the dependency onto `io.spine.tools:compiler-gradle-plugin`,
         * so that CoreJvm Gradle Plugin can add it to a project.
         */
        val compilerGradlePlugin = dependencyNode()
        compilerGradlePlugin.let {
            spineToolsGroup(it)
            artifactId(it, "compiler-gradle-plugin")
            version(it, Compiler.version)
            runtimeScope(it)
        }
        addExclusions(compilerGradlePlugin)

        /*
         * Add the dependency onto `io.spine.tools:compiler-gradle-api`,
         * so that CoreJvm Gradle Plugin can add it to a project.
         */
        val compilerGradleApi = dependencyNode()
        compilerGradleApi.let {
            spineToolsGroup(it)
            artifactId(it, "compiler-gradle-api")
            version(it, Compiler.version)
            runtimeScope(it)
        }
        addExclusions(compilerGradleApi)

        /*
         * Add the dependency onto `io.spine.tools:compiler-params`,
         * so that it is available in the classpath.
         */
        val compilerParams = dependencyNode()
        compilerParams.let {
            spineToolsGroup(it)
            artifactId(it, "compiler-params")
            version(it, Compiler.version)
            runtimeScope(it)
        }
        addExclusions(compilerParams)

        /*
         * Add the dependency onto `io.spine.tools:validation-java-bundle`.
         *
         * We filter out the content of the `io/spine/tools/validation/` directory
         * from the fat JAR artifact, so we need to add the dependency on the bundle.
         */
        val validationJavaBundle = dependencyNode()
        validationJavaBundle.let {
            spineToolsGroup(it)
            artifactId(it, "validation-java-bundle")
            version(it, Validation.version)
            runtimeScope(it)
        }
        addExclusions(validationJavaBundle)

        /*
         * Add the dependency onto `io.spine.tools:validation-java-bundle`.
         *
         * Similarly to the above, we need to add the dependency on
         * the Gradle plugin artifact as well.
         */
        val validationGradlePlugin = dependencyNode()
        validationGradlePlugin.let {
            spineToolsGroup(it)
            artifactId(it, "validation-gradle-plugin")
            version(it, Validation.version)
            runtimeScope(it)
        }
        addExclusions(validationGradlePlugin)

        /*
         * Add dependency onto `io.spine.tools:time-gradle-plugin`
         * because we exclude the code of Time Gradle plugin from the fat JAR artifact.
         */
        val timeGradlePlugin = dependencyNode()
        timeGradlePlugin.let {
            spineToolsGroup(it)
            artifactId(it, "time-gradle-plugin")
            version(it, Time.version)
            runtimeScope(it)
        }
        addExclusions(timeGradlePlugin)

        fun protobufGroup(parent: Node) = Node(parent, "groupId", Protobuf.group)

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
        dependencyNode().let {
            protobufGroup(it)
            artifactId(it, "protobuf-gradle-plugin")
            version(it, Protobuf.GradlePlugin.version)
            runtimeScope(it)
        }

        /*
         * Add the dependency on the Protobuf Java library so that we can add it
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
        dependencyNode().let {
            protobufGroup(it)
            artifactId(it, "protobuf-java")
            version(it, Protobuf.version)
            runtimeScope(it)
        }

        /*
         * Add the dependency on the Protobuf Java Util library because it is
         * used from the `compiler-params` module. Since we exclude the dependencies
         * on Protobuf, we need to add the Util library manually.
         *  The code in `pom.xml` would look like this:
         * ```
         * <dependency>
         *     <groupId>com.google.protobuf</groupId>
         *     <artifactId>protobuf-java-util</artifactId>
         *     <version>${Protobuf.version}</version>
         *     <scope>runtime</scope>
         * </dependency>
         * ```
         */
        dependencyNode().let {
            protobufGroup(it)
            artifactId(it, "protobuf-java-util")
            version(it, Protobuf.version)
            runtimeScope(it)
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
        dependencyNode().let {
            protobufGroup(it)
            artifactId(it, "protobuf-kotlin")
            version(it, Protobuf.version)
            runtimeScope(it)
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
        dependencyNode().let {
            Node(it, "groupId", Ksp.group)
            artifactId(it, Ksp.gradlePluginArtifactName)
            version(it, Ksp.version)
            runtimeScope(it)
        }
    }
}

fun excludeGroup(exclusions: Node, groupId: String) {
    Node(exclusions, "exclusion").let {
        Node(it, "groupId", groupId)
        Node(it, "artifactId", "*")
    }
}

// As defined in `versions.gradle.kts`.
// Do not publish to Gradle Plugin Portal snapshot versions.
// It is prohibited by their policy: https://plugins.gradle.org/docs/publish-plugin
val versionToPublish: String by extra

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

        // Strip `ArtifactMeta` for:
        "META-INF/io.spine/io.spine.tools_compiler-gradle-plugin.meta", // Compiler Gradle Plugin
        "META-INF/io.spine/io.spine.tools_protobuf-setup-plugins.meta", // Protobuf Setup Plugins

        // Strip code provided by the Spine Compiler CLI fat JAR.
        "android/**",
        "com/google/api/**",
        "com/google/apps/**",
        "com/google/cloud/**",
        "com/google/geo/**",
        "com/google/logging/**",
        "com/google/longrunning/**",
        "com/google/rpc/**",
        "com/google/shopping/**",
        "com/google/type/**",
        "com/palantir/**",
        "com/github/benmanes/caffeine/**",
        "io/grpc/**",
        "io/perfmark/**",
        "fj/**",
        "javax/annotation/**",

        // Strip the Validation library code generation code.
        // It is going to be available as runtime dependencies via `pom.xml`.
        "io/spine/tools/validation/**",

        // Strip the code of Time Gradle plugin.
        // It is going to be available via `pom.xml`.
        "io/spine/tools/time/**",

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
    )

    /* The archive has way too many items. So use the Zip64 mode. */
    isZip64 = true

    /* Prevent Gradle setting something like `osx-x86_64`. */
    archiveClassifier.set("")

    setup()
}

/**
 * Tests use the artifacts published to `mavenLocal`, so we need to publish first.
 */
tasks.test {
    dependsOn(rootProject.tasks.named("localPublish"))
}

/**
 * Wire `sourcesJar` dependencies explicitly.
 *
 * `java-gradle-plugin` (applied via `plugin-publish`) creates `sourcesJar` in its own
 * `afterEvaluate`, which runs after the `module` convention plugin's `afterEvaluate`
 * that wires common task dependencies. Using `configureEach` captures the task lazily
 * regardless of registration order and avoids implicit-dependency validation failures.
 */
tasks.withType<Jar>().configureEach {
    if (name == "sourcesJar") {
        listOf(
            "writeArtifactMeta",
            "prepareProtocConfigVersions",
            "kspKotlin",
        ).forEach { taskName ->
            tasks.findByName(taskName)?.let { dependsOn(it) }
        }
    }
}

tasks {
    // There are no public Java types in this module.
    // The task fails complaining about this fact.
    javadoc {
        enabled = false
    }
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

/**
 * Removes the `pluginMaven` publication auto-created by `java-gradle-plugin` (applied
 * transitively via `plugin-publish`) with the wrong `artifactId` equal to the project
 * name `"plugins"`, and fixes the `PluginMarkerMaven` POM so that it refers only to
 * the `core-jvm-plugins` fat JAR artifact.
 *
 * Root cause: `java-gradle-plugin` registers its own `afterEvaluate` callback that
 * creates a `pluginMaven` publication using `project.name` as the `artifactId`.
 * Calling `publications.clear()` during the configuration phase cannot prevent a publication
 * added by a later `afterEvaluate`, which is why that approach was abandoned in favour of
 * the `removeIf` call below.
 * As a result, both `pluginBundle` (`core-jvm-plugins`) and `pluginMaven` (`plugins`)
 * end up being published.  The same `afterEvaluate` also injects a dependency on
 * `plugins` into the marker POM, which must be replaced with `core-jvm-plugins`.
 */
afterEvaluate {
    val pluginPublication = "pluginMaven"
    publishing {
        publications.removeIf { it.name == pluginPublication }
    }
    // Removing the publication from the container does not remove the
    // already-created publish tasks that `maven-publish` wired reactively.
    // Disable them explicitly so that `publishToMavenLocal` (and other
    // repository targets) no longer execute `publishPluginMavenPublication*`.
    tasks.withType<PublishToMavenLocal>().configureEach {
        if (publication.name == pluginPublication) enabled = false
    }
    tasks.withType<PublishToMavenRepository>().configureEach {
        if (publication.name == pluginPublication) enabled = false
    }

    publishing {
        publications.withType<MavenPublication>().configureEach {
            if (name.endsWith("PluginMarkerMaven")) {
                pom.withXml {
                    val dependencies = dependenciesNode()
                    // Remove the dependency on `plugins` auto-generated by `java-gradle-plugin`.
                    val thisModuleName = "plugins"
                    (dependencies.children() as NodeList)
                        .filterIsInstance<Node>()
                        .filter { node ->
                            (node.get("artifactId") as? NodeList)
                                ?.filterIsInstance<Node>()
                                ?.any { it.text() == thisModuleName } == true
                        }
                        .forEach { dependencies.remove(it) }
                    // Add the correct dependency on the fat JAR artifact.
                    val dependency = Node(dependencies, "dependency")
                    dependency.let {
                        Node(it, "groupId", Spine.toolsGroup)
                        Node(it, "artifactId", CoreJvmCompiler.fatJarArtifact)
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
