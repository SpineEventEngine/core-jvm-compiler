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
import io.spine.dependency.lib.AutoService
import io.spine.dependency.lib.Kotlin
import io.spine.dependency.lib.KotlinPoet
import io.spine.dependency.lib.Protobuf
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
import io.spine.gradle.report.license.LicenseReporter
import org.gradle.api.artifacts.component.ModuleComponentIdentifier

plugins {
    module
    id("io.spine.artifact-meta")
    `maven-publish`
    `plugin-publish`
    `write-manifest`
}
LicenseReporter.generateReportIn(project)

/**
 * The ID used for publishing this module.
 *
 * The ID is declared here, and not in the `CoreJvmCompiler` dependency object,
 * because that object belongs to the `config` submodule, which does not know
 * about this artifact yet. Consumer projects of the CoreJvm Compiler will be
 * able to refer to it via `CoreJvmCompiler` once `config` catches up.
 */
val moduleArtifactId: String = "core-jvm-gradle-plugin"

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

    // Module dependencies.
    //
    // `:base` provides the `coreJvm` DSL and the settings protos of
    // the Compiler plugins. Its code reaches consumers inside the fat JAR
    // published by the `compiler-plugins` module; see `tuneDependencies()`
    // below.
    // `:grpc`, `:ksp`, and `:routing` provide no Compiler plugins and ship
    // inside the JAR of this module; see the `tasks.jar` configuration below.
    // The code-generation modules are referenced by name only;
    // see `CoreJvmCompilerPlugins`.
    listOf(
        ":base",
        ":grpc",
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

    // The code-generation modules, for `CoreJvmCompilerPluginsSpec` verifying
    // the class names declared by `CoreJvmCompilerPlugins`.
    listOf(
        ":annotation",
        ":entity",
        ":signal",
        ":marker",
        ":message-group",
        ":uuid",
        ":comparable"
    ).forEach {
        testImplementation(project(it))
    }
}

/**
 * Packs the classes and resources of the Gradle-runtime modules into
 * the JAR of this module.
 *
 * The `grpc`, `ksp`, and `routing` modules provide no Compiler plugins.
 * Their code — the Gradle-facing plugins and the routing KSP processor —
 * runs inside the Gradle runtime, so it belongs to this JAR and not to
 * the fat JAR assembled by the `compiler-plugins` module. `RoutingPlugin`
 * points KSP at this artifact; see its `mavenCoordinates` property.
 */
tasks.jar {
    listOf(":grpc", ":ksp", ":routing").forEach { module ->
        val moduleJar = project(module).tasks.named<Jar>("jar")
        from(zipTree(moduleJar.flatMap { it.archiveFile })) {
            exclude("META-INF/MANIFEST.MF")
            // Every module generates its own copy; this JAR carries its own.
            exclude("versions.properties")
        }
    }
}

publishing {
    publications {
        create("pluginJar", MavenPublication::class) {
            // `groupId` and `version` are filled in by `CustomPublicationHandler`.
            artifactId = moduleArtifactId
            artifact(tasks.jar)
            tuneDependencies()
        }
    }
}

/**
 * Adds the `runtime` dependency on the fat JAR artifact published by
 * the `compiler-plugins` module.
 *
 * The plugin classes in this thin JAR run on a consumer's build classpath
 * together with the code of the feature modules — e.g., the settings DSL of
 * `base`, or the Gradle-facing plugins of `grpc` and `routing` — which is
 * bundled into the fat JAR. The fat JAR POM, in turn, declares the curated
 * `runtime` dependencies on the Spine Compiler, Validation, Protobuf, KSP,
 * and Jackson artifacts, so consumers receive them transitively, exactly as
 * they did when the fat JAR itself served as the plugin artifact.
 *
 * The appended code in `pom.xml` would look like this:
 * ```
 * <dependency>
 *     <groupId>io.spine.tools</groupId>
 *     <artifactId>core-jvm-plugins</artifactId>
 *     <version>${project.version}</version>
 *     <scope>runtime</scope>
 * </dependency>
 * ```
 */
private fun MavenPublication.tuneDependencies() {
    // Capture the value during the configuration phase: the `withXml` action
    // runs when the POM is generated, and must not reach out to `project`.
    val fatJarVersion = project.version.toString()
    pom.withXml {
        val projectNode = asNode()
        val dependencies = Node(projectNode, "dependencies")
        Node(dependencies, "dependency").let {
            Node(it, "groupId", Spine.toolsGroup)
            Node(it, "artifactId", CoreJvmCompiler.compilerPluginsArtifact)
            Node(it, "version", fatJarVersion)
            Node(it, "scope", "runtime")
        }

        /*
         * The routing KSP processor shipped in this JAR uses KotlinPoet
         * when generating code. The library comes to the KSP classpath as
         * a genuine artifact rather than being bundled. The Kotlin runtime
         * is excluded because the Gradle and KSP runtimes provide it.
         */
        Node(dependencies, "dependency").let {
            val (group, name, version) = KotlinPoet.ksp.split(':')
            Node(it, "groupId", group)
            Node(it, "artifactId", name)
            Node(it, "version", version)
            Node(it, "scope", "runtime")
            Node(it, "exclusions").let { exclusions ->
                Node(exclusions, "exclusion").let { exclusion ->
                    Node(exclusion, "groupId", "org.jetbrains.kotlin")
                    Node(exclusion, "artifactId", "*")
                }
            }
        }
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
    dependsOn(publishPlugins)
}

/**
 * Third-party artifacts resolved by the stub projects which the tests of
 * this module create via `ProjectBuilder`.
 *
 * Stub projects cannot reuse the dependency cache of the machine —
 * `ProjectBuilder` runs on in-memory caches — and are forbidden to go to
 * the network; see `StubResolution.kt` in the test fixtures of the `base`
 * module. The `prepareStubRepo` task below serves this configuration to
 * the stub projects as a local Maven repository. When a test fails with
 * "No cached version available for offline mode", add the missing artifact
 * here.
 */
val stubRepoDeps: Configuration = configurations.create("stubRepoDeps") {
    // The configuration is resolve-only; the legacy `create` defaults to consumable.
    isCanBeConsumed = false
}

dependencies {
    // Added to consumer projects by `CommonKspSettingsPlugin` of the `ksp` module.
    stubRepoDeps(AutoService.annotations)

    // Added to consumer projects by the Time Gradle Plugin.
    stubRepoDeps(Time.lib)
    stubRepoDeps(Time.javaExtensions)

    // Added to consumer projects by the routing infrastructure; see `Meta.kt` in `base`.
    stubRepoDeps(CoreJvm.server)
    stubRepoDeps(CoreJvm.client)

    // Added to consumer projects by the Validation Gradle Plugin.
    stubRepoDeps(Validation.runtime)
}

/**
 * Assembles a local Maven repository with [stubRepoDeps] for the stub
 * projects created by the tests of this module.
 *
 * The repository holds plain artifact files in the Maven directory layout,
 * without POMs; stub projects read it with the `artifact()` metadata sources.
 * See `applyStandard()` in `StandardRepos.kt` of the test sources.
 */
val prepareStubRepo = tasks.register("prepareStubRepo") {
    description = "Assembles a local Maven repository for the stub projects of the tests"
    group = SpineTaskGroup.name
    val artifacts = stubRepoDeps.incoming.artifacts.resolvedArtifacts
    inputs.files(stubRepoDeps).withPropertyName("stubRepoDeps")
    val repoDir = layout.buildDirectory.dir("stub-repo")
    outputs.dir(repoDir).withPropertyName("repoDir")
    doLast {
        val rootDir = repoDir.get().asFile
        artifacts.get().forEach { artifact ->
            val id = artifact.id.componentIdentifier as? ModuleComponentIdentifier
                ?: return@forEach
            val moduleDir = rootDir.resolve(id.group.replace('.', '/'))
                .resolve(id.module)
                .resolve(id.version)
            moduleDir.mkdirs()
            artifact.file.copyTo(moduleDir.resolve(artifact.file.name), overwrite = true)
        }
    }
}

/**
 * Tests use the artifacts published to `mavenLocal`, so we need to publish first.
 */
tasks.test {
    dependsOn(rootProject.tasks.named("localPublish"))
    dependsOn(prepareStubRepo)
    // The property name is defined by `STUB_REPOSITORY_PROPERTY` in `StubResolution.kt`.
    systemProperty("stub.repository", layout.buildDirectory.dir("stub-repo").get().asFile.path)
    // Lets TestKit fixtures pin the Protobuf runtime at the refreshed version.
    systemProperty("protobuf.version", io.spine.dependency.lib.Protobuf.version)
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
 * name `"gradle-plugin"`, and fixes the `PluginMarkerMaven` POM so that it refers only
 * to the `core-jvm-gradle-plugin` artifact.
 *
 * Root cause: `java-gradle-plugin` registers its own `afterEvaluate` callback that
 * creates a `pluginMaven` publication using `project.name` as the `artifactId`.
 * Calling `publications.clear()` during the configuration phase cannot prevent a publication
 * added by a later `afterEvaluate`, which is why that approach was abandoned in favour of
 * the `removeIf` call below.
 * As a result, both `pluginJar` (`core-jvm-gradle-plugin`) and `pluginMaven`
 * (`gradle-plugin`) end up being published.  The same `afterEvaluate` also injects
 * a dependency on `gradle-plugin` into the marker POM, which must be replaced with
 * `core-jvm-gradle-plugin`.
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
        val pluginVersion = project.version.toString()
        publications.withType<MavenPublication>().configureEach {
            if (name.endsWith("PluginMarkerMaven")) {
                pom.withXml {
                    val dependencies = dependenciesNode()
                    // Remove the dependency on `gradle-plugin` auto-generated
                    // by `java-gradle-plugin`.
                    val thisModuleName = "gradle-plugin"
                    (dependencies.children() as NodeList)
                        .filterIsInstance<Node>()
                        .filter { node ->
                            (node.get("artifactId") as? NodeList)
                                ?.filterIsInstance<Node>()
                                ?.any { it.text() == thisModuleName } == true
                        }
                        .forEach { dependencies.remove(it) }
                    // Add the correct dependency on the plugin artifact.
                    val dependency = Node(dependencies, "dependency")
                    dependency.let {
                        Node(it, "groupId", Spine.toolsGroup)
                        Node(it, "artifactId", moduleArtifactId)
                        Node(it, "version", pluginVersion)
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
