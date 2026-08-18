/*
 * Copyright 2026, TeamDev. All rights reserved.
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
import io.spine.dependency.kotlinx.AtomicFu
import io.spine.dependency.kotlinx.Coroutines
import io.spine.dependency.kotlinx.DateTime
import io.spine.dependency.lib.Aedile
import io.spine.dependency.lib.Jackson
import io.spine.dependency.lib.JacksonV2
import io.spine.dependency.lib.JetBrainsAnnotations
import io.spine.dependency.lib.Kotlin
import io.spine.dependency.lib.Protobuf
import io.spine.dependency.local.BaseTypes
import io.spine.dependency.local.Change
import io.spine.dependency.local.Compiler
import io.spine.dependency.local.CoreJvm
import io.spine.dependency.local.CoreJvmCompiler
import io.spine.dependency.local.Spine
import io.spine.dependency.local.TestLib
import io.spine.dependency.local.Time
import io.spine.dependency.local.ToolBase
import io.spine.dependency.local.Validation
import io.spine.gradle.SpineTaskGroup
import io.spine.gradle.isSnapshot
import io.spine.gradle.publish.setup
import io.spine.gradle.report.license.LicenseReporter
import java.util.jar.JarFile

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
        implementation(project(it)) {
            excludeJetBrainsAnnotations()
        }
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
            addExclusions(it)
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
        val compilerJvm = dependencyNode()
        compilerJvm.let {
            spineToolsGroup(it)
            artifactId(it, "compiler-jvm")
            version(it, Compiler.version)
            runtimeScope(it)
            addExclusions(it)
        }

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
            addExclusions(it)
        }

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
            addExclusions(it)
        }

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
            addExclusions(it)
        }

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
            addExclusions(it)
        }

        /*
         * Add the dependency onto `io.spine.tools:validation-gradle-plugin`.
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
            addExclusions(it)
        }

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
            addExclusions(it)
        }

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

        /*
         * Add the Jackson libraries used at runtime by the code we bundle.
         *
         * Their classes are excluded from the fat JAR — see `pomProvidedModules`
         * near `tasks.shadowJar` — so that consumers receive genuine artifacts
         * that they can upgrade without waiting for a new release of
         * CoreJvm Compiler. SnakeYAML and SnakeYAML Engine are not listed here:
         * they come transitively, with the `jackson-dataformat-yaml` artifacts.
         */
        listOf(
            // Jackson 3.x, used by our own code.
            Jackson.core to Jackson.version,
            Jackson.databind to Jackson.version,
            Jackson.moduleKotlin to Jackson.version,
            Jackson.DataFormat.yaml to Jackson.version,
            Jackson.DataType.guava to Jackson.version,

            // The annotations artifact of the 2.x line, consumed by both lines.
            Jackson.annotations.substringBeforeLast(':') to Jackson.annotationsVersion,

            // Jackson 2.x, pulled by the third-party code we bundle.
            JacksonV2.Core.core to JacksonV2.version,
            JacksonV2.Core.databind to JacksonV2.version,
            JacksonV2.DataFormat.yaml to JacksonV2.version,
            JacksonV2.DataType.guava to JacksonV2.version,
            JacksonV2.DataType.jdk8 to JacksonV2.version,
            JacksonV2.Module.parameterNames to JacksonV2.version,
        ).forEach { (module, moduleVersion) ->
            val (group, name) = module.split(':')
            dependencyNode().let {
                Node(it, "groupId", group)
                artifactId(it, name)
                version(it, moduleVersion)
                runtimeScope(it)
                addExclusions(it)
            }
        }
    }
}

fun excludeGroup(exclusions: Node, groupId: String) {
    Node(exclusions, "exclusion").let {
        Node(it, "groupId", groupId)
        Node(it, "artifactId", "*")
    }
}

// As defined in `version.gradle.kts`.
// Do not publish to Gradle Plugin Portal snapshot versions.
// It is prohibited by their policy: https://plugins.gradle.org/docs/publish-plugin
val versionToPublish: String = extra["versionToPublish"] as String

val publishPlugins = tasks.named("publishPlugins") {
    enabled = !versionToPublish.isSnapshot()
}

tasks.publish {
    dependsOn(tasks.shadowJar)
    dependsOn(publishPlugins)
}

/**
 * Obtains the `group:name` part of a Maven coordinate,
 * which may or may not carry the version part.
 */
fun moduleOf(coordinate: String): String =
    coordinate.split(':').let { "${it[0]}:${it[1]}" }

/**
 * The `group:name` coordinates of the IntelliJ Platform artifacts coming from ToolBase.
 */
val intellijPlatformArtifacts: Set<String> = setOf(
    ToolBase.intellijPlatform,
    ToolBase.intellijPlatformJava,
).mapTo(mutableSetOf(), ::moduleOf)

/**
 * Modules whose content must never be bundled, even though they are present
 * on the runtime classpath outside the IntelliJ Platform dependency graph.
 *
 * Their classes are already excluded by the path patterns in `tasks.shadowJar`.
 * Excluding the whole modules also keeps their resource entries — Kotlin module
 * metadata, `ServiceLoader` registrations, embedded IntelliJ Platform
 * descriptors — out of the fat JAR.
 *
 * The Kotlin runtime libraries come with the Gradle runtime. The KSP artifacts
 * come with the KSP Gradle plugin declared in `pom.xml`. The JetBrains
 * annotations are compile-time only.
 */
val runtimeProvidedModules: Set<String> = buildSet {
    addAll(Kotlin.StdLib.modules)
    add(Kotlin.reflect)
    addAll(Coroutines.modules)
    add("${JetBrainsAnnotations.groupId}:${JetBrainsAnnotations.artifactId}")
    add(Ksp.symbolProcessingAaEmb)
}

/**
 * Modules excluded from the fat JAR in favor of the `runtime` dependencies
 * declared in `pom.xml`; see `tuneDependencies()` above.
 *
 * The set is intentionally wider than the list in `pom.xml`. Whole module
 * families are excluded here, while `pom.xml` declares only the artifacts
 * whose classes used to be bundled. For a family member absent from
 * the runtime classpath — e.g. `jackson-dataformat-xml` — the exclusion
 * is a no-op. A member that only the IntelliJ Platform artifacts bring —
 * e.g. `jackson-jr-objects` — is excluded by the dependency filter anyway.
 *
 * Consumers receive these libraries as ordinary Maven artifacts, so they can
 * upgrade them via the standard dependency resolution without waiting for
 * a new version of CoreJvm Compiler.
 */
val pomProvidedModules: Set<String> = buildSet {
    // Jackson 3.x.
    add(Jackson.core)
    add(Jackson.databind)
    add(Jackson.moduleKotlin)
    add(Jackson.DataFormat.yaml)
    add(Jackson.DataType.guava)

    // Jackson 2.x, with the `jackson-annotations` artifact shared by both lines.
    add(Jackson.annotations.substringBeforeLast(':'))
    addAll(JacksonV2.Core.modules)
    addAll(JacksonV2.DataFormat.modules)
    addAll(JacksonV2.DataType.modules)
    addAll(JacksonV2.Module.modules)
    addAll(JacksonV2.Junior.modules)

    /*
     * Not declared in `pom.xml` explicitly: these come to consumers
     * transitively, with the `jackson-dataformat-yaml` artifacts declared
     * there. There are no dependency objects for them because no Spine
     * module declares them directly.
     */
    add("org.yaml:snakeyaml")
    add("org.snakeyaml:snakeyaml-engine")

    /*
     * These come to consumers transitively, with `protobuf-gradle-plugin`
     * declared in `pom.xml`. There are no dependency objects for them
     * because no Spine module declares them directly.
     */
    add("com.google.gradle:osdetector-gradle-plugin")
    add("kr.motd.maven:os-maven-plugin")
}

/**
 * Modules provided by the `io.spine.tools:compiler-cli` fat JAR — the platform
 * in whose classpath the CoreJvm Compiler plugins run.
 *
 * Each module listed here is fully contained in the CLI fat JAR, and
 * the Gradle-plugin side of this artifact — running on consumers' build
 * classpath, where the platform is absent — does not use it.
 *
 * When the Compiler starts publishing the platform contract — a BOM
 * enumerating what the CLI fat JAR embeds — this list should be derived
 * from that contract instead of being maintained by hand.
 */
val cliProvidedModules: Set<String> = buildSet {
    // The CoreJvm framework, referenced by the generated code
    // and by the code-generation plugins.
    add(moduleOf(CoreJvm.core))
    add(moduleOf(CoreJvm.client))
    add(moduleOf(CoreJvm.server))
    add(moduleOf(Change.lib))
    add(moduleOf(BaseTypes.lib))
    add(moduleOf(Time.lib))
    add(moduleOf(Time.javaExtensions))

    // The PSI infrastructure of ToolBase, used at code generation time only.
    add(moduleOf(ToolBase.psi))
    add(moduleOf(ToolBase.psiJava))

    // The Spine Compiler modules; consumers' build classpath receives them
    // via the `runtime` dependencies declared in `pom.xml`.
    add(moduleOf(Compiler.api))
    add(moduleOf(Compiler.jvm))
    add(moduleOf(Compiler.backend))
    add(moduleOf(Compiler.params))

    /*
     * Caching and date-time libraries of the code-generation stack.
     * Kotlin Multiplatform artifacts resolve to their `-jvm` counterparts,
     * so both module names are listed.
     */
    add(moduleOf(Aedile.lib))
    add(moduleOf(DateTime.lib))
    add("${moduleOf(DateTime.lib)}-jvm")
    addAll(AtomicFu.modules)
    add("${AtomicFu.group}:${AtomicFu.module}-jvm")

    // Annotation-only libraries referenced by the modules above.
    add("com.google.auto.service:auto-service-annotations")
    add("org.codehaus.mojo:animal-sniffer-annotations")
}

/**
 * Obtains the `group:name` coordinates of a module component,
 * or `null` if this component is a project.
 */
fun ResolvedComponentResult.moduleKey(): String? =
    (id as? ModuleComponentIdentifier)?.run { "$group:$module" }

/**
 * Calculates the modules that are present on the runtime classpath only because
 * the IntelliJ Platform artifacts depend on them.
 *
 * The function traverses the resolved runtime classpath graph without following
 * the dependency edges of the IntelliJ Platform artifacts, and returns the modules
 * of the full graph that were not visited, plus the artifacts themselves.
 *
 * A module that our own code needs as well — e.g. Guava, which is also among
 * the IntelliJ Platform dependencies — stays reachable via other graph edges,
 * and so does not get into the result.
 */
fun intellijPlatformOnlyModules(): Set<String> {
    val resolution = configurations.runtimeClasspath.get().incoming.resolutionResult
    val visited = mutableSetOf<ComponentIdentifier>()
    val reachable = mutableSetOf<String>()
    val queue = ArrayDeque(listOf(resolution.root))
    while (queue.isNotEmpty()) {
        val component = queue.removeFirst()
        if (!visited.add(component.id)) {
            continue
        }
        val module = component.moduleKey()
        if (module in intellijPlatformArtifacts) {
            continue
        }
        module?.let(reachable::add)
        component.dependencies.asSequence()
            .filterIsInstance<ResolvedDependencyResult>()
            .filterNot { it.isConstraint }
            .forEach { queue.add(it.selected) }
    }
    val allModules = resolution.allComponents.mapNotNull { it.moduleKey() }
    return allModules.toSet() - reachable
}

tasks.shadowJar {
    /*
     * Exclude the IntelliJ Platform artifacts and their dependencies from the fat JAR.
     * They are present on the runtime classpath transitively, via `psi-java` of ToolBase,
     * and are of no use for the Gradle plugins shipped in this JAR.
     *
     * The exclusion is keyed off the declared module coordinates rather than off
     * the entry names found inside the IntelliJ Platform JARs. A content-derived
     * exclusion silently inverts when upstream changes what it bundles: once
     * the IntelliJ Platform artifacts stopped fat-jarring their third-party code
     * and declared it as ordinary POM dependencies, the entry-based rule excluded
     * nothing, and the whole dependency set leaked into this JAR unrelocated,
     * shadowing genuine artifacts on consumers' build classpaths.
     *
     * The `verifyBundledPackages` task below guards this invariant.
     *
     * The classpath graph is queried lazily, when the task runs, to avoid
     * resolution during the configuration phase.
     */
    val intellijPlatformModules by lazy { intellijPlatformOnlyModules() }
    dependencies {
        exclude { dependency ->
            val module = "${dependency.moduleGroup}:${dependency.moduleName}"
            module in runtimeProvidedModules
                    || module in pomProvidedModules
                    || module in cliProvidedModules
                    || module in intellijPlatformModules
        }
    }

    exclude(
        /*
         * Exclude Kotlin runtime because it will be provided by the Gradle runtime.
         */
        "kotlin/**",
        "META-INF/versions/*/kotlin/**", // Multi-release copies of the above.

        /*
         * Exclude Coroutines. They also will be present. The rest of `kotlinx` should stay.
         */
        "kotlinx/coroutines/**",

        // Debug metadata of the Coroutines library, which lives outside `kotlinx/`.
        "_COROUTINE/**",

        /*
         * Kotlin runtime metadata and service files, embedded in some of
         * the artifacts we do bundle, e.g. `protobuf-setup-plugins` of ToolBase.
         * The libraries these files describe are not bundled; see above.
         */
        "META-INF/kotlin-*.kotlin_module",
        "META-INF/annotations.kotlin_module",
        "META-INF/compiler.common*.kotlin_module",
        "META-INF/descriptors*.kotlin_module",
        "META-INF/deserialization*.kotlin_module",
        "META-INF/metadata*.kotlin_module",
        "META-INF/util.runtime.kotlin_module",
        "META-INF/services/kotlin.reflect.*",

        /*
         * Protobuf runtime and Gradle plugin will be available in the classpath because
         * fat JAR has the Maven `runtime` dependency on it.
         * Please see manipulations with `pom.xml` in the `publishing` block above.
         */
        "com/google/protobuf/**",
        "META-INF/gradle-plugins/com.google.protobuf.properties",

        /*
         * Gson comes to consumers with `protobuf-java-util` via the `runtime`
         * dependency in `pom.xml`, similarly to the Protobuf entries above.
         */
        "com/google/gson/**",

        /*
         * Strip annotation-only libraries pulled in transitively, e.g. by Guava.
         * The plugins do not need them at runtime.
         */
        "com/google/errorprone/**",
        "com/google/j2objc/**",
        "org/intellij/**",
        "org/jetbrains/annotations/**",
        "org/jspecify/**",

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

        /*
         * JNA classes come embedded, unrelocated, in `roaster-jdt`, which we bundle.
         * The plugins do not use JNA, and a partial copy of it could shadow
         * the genuine JNA artifact on a consumer's build classpath.
         */
        "com/sun/jna/**",

        /*
         * OS detection classes come embedded, unrelocated, in
         * `protobuf-setup-plugins` of ToolBase, which we bundle. Consumers
         * receive the genuine artifacts transitively, with
         * `protobuf-gradle-plugin` declared in `pom.xml`.
         */
        "com/google/gradle/**",
        "kr/motd/**",

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
        "META-INF/versions/*/ksp/**", // Multi-release copies of the above.
        "com/google/devtools/ksp/**",

        // Do not declare third-party Gradle plugins,
        // especially those stripped above.
        "META-INF/gradle-plugins/com.google**",

        // Exclude license files that cause or may cause issues with LicenseReport.
        // We analyze these files when building artifacts we depend on.
        "about_files/**",
        "license/**",
        "META-INF/LICENSE*",
        "META-INF/NOTICE*",
        "META-INF/AL2.0",
        "META-INF/LGPL2.1",

        // Maven metadata and Android tooling rules of the merged libraries.
        "META-INF/maven/**",
        "META-INF/proguard/**",
        "META-INF/com.android.tools/**",

        "ant_tasks/**", // `resource-ant.jar` is of no use here.

        /* Exclude `https://github.com/JetBrains/pty4j`.
          We don't need the terminal. */
        "resources/com/pty4j/**",

        /*
         * IntelliJ Platform registry defaults embedded in `psi-java` of ToolBase.
         * The genuine IntelliJ Platform artifacts provide this file
         * on the Spine Compiler classpath.
         */
        "misc/registry.properties",

        // Protobuf files.
        "google/**",
        "spine/**",
        "src/**",

        // Java source code files of the package `org.osgi`.
        "OSGI-OPT/**",

        // OSGi manifest fragments, including copies in multi-release JARs.
        "OSGI-INF/**",
        "META-INF/versions/*/OSGI-INF/**",
    )

    /* The archive has way too many items. So use the Zip64 mode. */
    isZip64 = true

    /* Prevent Gradle setting something like `osx-x86_64`. */
    archiveClassifier.set("")

    setup()
}

/**
 * The package prefixes of the class files expected in the fat JAR.
 *
 * The list feeds the `verifyBundledPackages` task below. When a new runtime
 * dependency must be bundled, extend the list consciously: every bundled class
 * is served unrelocated, so it shadows a class with the same name coming from
 * a genuine artifact on a consumer's build classpath.
 */
val expectedPackages = listOf(
    // The code of this repository and the ToolBase infrastructure
    // used by the Gradle-plugin side.
    "io/spine/tools/",

    /*
     * The Spine base libraries used by the Gradle-plugin side.
     *
     * The heavier framework — `spine-core`, `spine-client`, `spine-server`,
     * and friends — must not appear here: the code-generation plugins meet it
     * inside the Compiler CLI classpath; see `cliProvidedModules`.
     */
    "io/spine/annotation/",
    "io/spine/base/",
    "io/spine/code/",
    "io/spine/collect/",
    "io/spine/compare/",
    "io/spine/environment/",
    "io/spine/format/",
    "io/spine/io/",
    "io/spine/logging/",
    "io/spine/option/",
    "io/spine/protobuf/",
    "io/spine/query/",
    "io/spine/reflect/",
    "io/spine/security/",
    "io/spine/string/",
    "io/spine/type/",
    "io/spine/util/",
    "io/spine/validate/",
    "io/spine/validation/",
    "io/spine/value/",

    // Guava is bundled deliberately: the plugins need its types at runtime.
    "com/google/common/",
    "com/google/thirdparty/", // The public suffix data of Guava.

    // Java and Kotlin code generation.
    "com/squareup/javapoet/",
    "com/squareup/kotlinpoet/",

    // Roaster, for parsing and formatting Java sources.
    "org/jboss/forge/",
)

/**
 * Verifies that the fat JAR contains only class files of [expectedPackages].
 *
 * The task guards against silent re-appearance of third-party code in the fat JAR,
 * like the one that happened when the IntelliJ Platform artifacts stopped bundling
 * their dependencies and declared them as ordinary POM dependencies instead.
 * See the dependency filter in `tasks.shadowJar` above for details.
 */
val verifyBundledPackages = tasks.register("verifyBundledPackages") {
    description = "Verifies that the fat JAR bundles only classes of expected packages"
    group = SpineTaskGroup.name
    val archive = tasks.shadowJar.flatMap { it.archiveFile }
    inputs.file(archive).withPathSensitivity(PathSensitivity.NONE)
    val marker = layout.buildDirectory.file("verifyBundledPackages/verified.txt")
    outputs.file(marker)
    doLast {
        val multiRelease = Regex("^META-INF/versions/\\d+/")
        val offenders = JarFile(archive.get().asFile).use { jar ->
            jar.entries().asSequence()
                .map { it.name }
                .filter { it.endsWith(".class") }
                .map { it.replaceFirst(multiRelease, "") }
                .filterNot { name -> expectedPackages.any { name.startsWith(it) } }
                .sorted()
                .toList()
        }
        if (offenders.isNotEmpty()) {
            val preview = offenders.take(30).joinToString(System.lineSeparator())
            throw GradleException(
                "The fat JAR contains ${offenders.size} class file(s)" +
                        " outside the expected packages, e.g.:" +
                        System.lineSeparator() + preview
            )
        }
        marker.get().asFile.run {
            parentFile.mkdirs()
            writeText("ok")
        }
    }
}

tasks.shadowJar {
    finalizedBy(verifyBundledPackages)
}

tasks.check {
    dependsOn(verifyBundledPackages)
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
