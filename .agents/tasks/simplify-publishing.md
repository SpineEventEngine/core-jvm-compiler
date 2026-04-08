# Task: Simplify the publishing process

## Introduction

- Now there is only one module of this project which is published. It is named `plugins`.
- The published artifact is `io.spine.tools:core-jvm-plugins:<version>`.
- This artifact is a fat JAR produced by Shadow Gradle plugin.
- This is a customly published module, which is controlled by tuning of `spinePublishing`
  extension in the root project `build.gradle.kts` file.
- The code which is really published belongs to the `gradle-plugins` module. This module
  is the `implementation` dependency of the `plugins` module.
- Thus, the `plugins` module is a publishing-only "wrapper" for the `gradle-plugins` module,
  which is published as a fat JAR.
- Having a publish-only module is confusing and is a maintenance problem.

## Goals

- Merge the `gradle-plugins` module into the `plugins` module,
  so that there is only one module which is published and contains the code
  which is really published.
- After the merge, completely remove the `gradle-plugins` module directory and remove it
  from `settings.gradle.kts`.

## Constraints

- Preserve the Git history.
- Move the files instead of deleting and creating new ones (use `git mv`).

## Current Structure (before merge)

### `gradle-plugins` module — contains all production code

Main sources:
- `gradle-plugins/src/main/kotlin/io/spine/tools/core/jvm/gradle/plugins/`
  - `CleaningPlugin.kt`
  - `CompilerConfigPlugin.kt`
  - `CoreJvmPlugin.kt`
  - `Meta.kt`
  - `WriteCompilerPluginsSettings.kt`
- `gradle-plugins/src/main/java/io/spine/tools/core/jvm/gradle/plugins/package-info.java`
- `gradle-plugins/src/main/resources/META-INF/gradle-plugins/io.spine.core-jvm.properties`

Test sources:
- `gradle-plugins/src/test/java/io/spine/tools/core/jvm/gradle/`
  - `CoreJvmPluginSpec.java`
  - `GradleProjects.java`
  - `TempArtifactDirsTest.java`
  - `given/StubProject.java`
  - `given/package-info.java`
- `gradle-plugins/src/test/kotlin/io/spine/tools/core/jvm/`
  - `StandardRepos.kt`
  - `gradle/plugins/CoreJvmOptionsSpec.kt`
  - `gradle/plugins/CoreJvmPluginSpec.kt`

Build script (`gradle-plugins/build.gradle.kts`):
- Applies `module` and `io.spine.artifact-meta` plugins.
- Sets `artifactId` to `CoreJvmCompiler.fatJarArtifact` via `artifactMeta { }`.
- `artifactMeta { }` adds Validation, Kotlin Gradle Plugin, Protobuf Gradle Plugin, KSP
  as declared POM dependencies, and excludes build tool configurations.
- Declares implementation dependencies: `Compiler.pluginLib`, `Compiler.params`,
  `ToolBase.jvmTools`, `Validation.gradlePluginLib`, `Protobuf.GradlePlugin.lib` (compileOnly).
- Declares compile-only: `gradleKotlinDsl()`, `Protobuf.GradlePlugin.lib`.
- Declares module implementation dependencies on sibling modules:
  `:base`, `:annotation`, `:entity`, `:grpc`, `:signal`, `:marker`,
  `:message-group`, `:uuid`, `:comparable`, `:routing`.
- Declares test dependencies: `gradleApi()`, `gradleKotlinDsl()`, `gradleTestKit()`,
  `Kotlin.GradlePlugin.lib`, `TestLib.lib`, `ToolBase.pluginTestlib`,
  `testFixtures(project(":base"))`.
- `test` task depends on `rootProject.tasks.named("localPublish")`.
- `javadoc` task is disabled (no Java types in the module).

### `plugins` module — publishing-only wrapper

- No production source code.
- One integration test: `plugins/src/test/kotlin/.../CoreJvmPluginIgTest.kt`.
- Build script (`plugins/build.gradle.kts`):
  - Applies `maven-publish`, `com.gradleup.shadow`, `plugin-publish`, `write-manifest`.
  - `implementation(project(":gradle-plugins"))` — the only production dependency.
  - Shadow JAR configuration with exclusions for IntelliJ Platform, Kotlin runtime, Protobuf, etc.
  - Manual POM manipulation via `tuneDependencies()` to declare runtime dependencies.
  - `afterEvaluate` block that disables `sourcesJar` and `javadocJar`
    (because there is no source code).
  - `tasks.jar { enabled = false }` with comment "This module is for publishing only".
  - `disableDocumentationTasks()` call.
  - Gradle plugin registration (`gradlePlugin { }` block).
  - `afterEvaluate` block to remove the auto-created `pluginMaven` publication and fix
    the marker POM so it refers to `core-jvm-plugins` artifact.
  - `test` task depends on `rootProject.tasks.named("localPublish")`.

## Steps

### 1. Move source files using `git mv`

Move all production and test sources from `gradle-plugins` into `plugins`:

```
git mv gradle-plugins/src/main/kotlin  plugins/src/main/kotlin
git mv gradle-plugins/src/main/java    plugins/src/main/java
git mv gradle-plugins/src/main/resources/META-INF  plugins/src/main/resources/META-INF
git mv gradle-plugins/src/test         plugins/src/test
```

After this, `plugins/src/test/` will contain both the existing `CoreJvmPluginIgTest.kt`
(integration test) and all tests moved from `gradle-plugins`.

### 2. Update `plugins/build.gradle.kts`

- **Add `module` plugin** — required for Kotlin compilation now that there is source code.
- **Add `id("io.spine.artifact-meta")` plugin**.
- **Add `artifactMeta { }` block** from `gradle-plugins/build.gradle.kts` alongside the
  existing `tuneDependencies()` POM manipulation (keep both).
- **Replace `implementation(project(":gradle-plugins"))`** with the actual implementation
  dependencies from `gradle-plugins/build.gradle.kts`:
  - `Compiler.pluginLib`, `Compiler.params`, `ToolBase.jvmTools`, `Validation.gradlePluginLib`
  - All sibling module `project()` dependencies:
    `:base`, `:annotation`, `:entity`, `:grpc`, `:signal`, `:marker`,
    `:message-group`, `:uuid`, `:comparable`, `:routing`
- **Add compile-only dependencies**: `gradleKotlinDsl()`, `Protobuf.GradlePlugin.lib`.
- **Merge test dependencies** — add those from `gradle-plugins` not already present in `plugins`:
  `gradleApi()`, `gradleKotlinDsl()`, `Kotlin.GradlePlugin.lib`, `testFixtures(project(":base"))`.
- **Remove the `afterEvaluate` block** that disables `sourcesJar` and `javadocJar`
  (there will be source code now).
- **Remove `tasks.jar { enabled = false }`** and its "publishing only" comment.
- **Review `disableDocumentationTasks()`**: the `gradle-plugins` module disabled `javadoc`
  because there are no Java types. The merged module will have `package-info.java` but
  the primary code is Kotlin. Keep documentation tasks disabled or update as appropriate.

### 3. Update `settings.gradle.kts`

Remove `"gradle-plugins"` from the `include()` list.

### 4. Remove the `gradle-plugins` directory

After all files are moved and build scripts are updated:

```
git rm -r gradle-plugins
```

### 5. Verify

- The project builds: `./gradlew :plugins:build`
- Tests pass: `./gradlew :plugins:test`
- Fat JAR is produced: `./gradlew :plugins:shadowJar`
- Publishing works: `./gradlew :plugins:publishToMavenLocal`
