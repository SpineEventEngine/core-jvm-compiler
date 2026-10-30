---
slug: unbundle-tool-base
branch: unbundle-tool-base
owner: claude
status: in-review
started: 2026-10-29
---

## Goal

The `core-jvm-plugins` fat JAR stops bundling ToolBase classes. Consumers receive
the ToolBase artifacts through `pom.xml`, so Gradle resolves one version of each
for the whole build classpath, and the Spine Compiler Gradle plugin runs against
the ToolBase it declares.

## Context

- The fat JAR bundles eleven ToolBase modules unrelocated: `plugin-base`,
  `gradle-plugin-api`, `root-gradle-plugins`, `protobuf-setup-plugins`,
  `jvm-tools`, `code`, `fs`, `archive`, `java-code`, `proto-code`,
  `classic-codegen`. Relocation is not an option: the plugins share
  ToolBase types such as `LibraryPlugin` and the `spine { }` extension.
- Up to ToolBase `.430`, `protobuf-setup-plugins` was an uber JAR embedding
  Guava, Gson, the Protobuf Gradle plugin, Spine Base and Logging, and other
  ToolBase modules. The fat JAR got its Guava and `io/spine/*` copies from it.
  ToolBase `.440` publishes it as a plain JAR
  (SpineEventEngine/tool-base#200, SpineEventEngine/tool-base#201).
- `classic-codegen` brings JavaPoet and Roaster, which the fat JAR keeps
  bundling. Its POM entry excludes them, so `pom.xml` does not duplicate them.

## Plan

- [x] Pull `config`; bump `local` pins (ToolBase `.430`, CoreJvm `.610`)
- [x] Bump the version to `2.0.0-SNAPSHOT.100`
- [x] `compiler-plugins/build.gradle.kts`
  - [x] Declare the eleven ToolBase artifacts in `pomDependencies`
  - [x] Drop `bundledDespiteCli`; the POM now delivers those modules
  - [x] Narrow `expectedPackages` to `io/spine/tools/core/` and prune entries
        that no longer get bundled, so the guard flags ToolBase leaks
  - [x] Keep the order of the POM exclusions deterministic
  - [x] Update the comments that describe ToolBase as bundled; drop the path
        exclusions whose only source was `protobuf-setup-plugins`
- [x] Update the KDoc of `tuneDependencies()` in `gradle-plugin/build.gradle.kts`
- [x] Verify: fat JAR content, generated POM, `./gradlew build`, `tests/` build

## Log

- 2026-10-29 — investigated, plan drafted, executing
- 2026-10-29 — implemented; `clean build --no-build-cache` green, including
  `integrationTests` (101 tests). The fat JAR shrank from 18.2 MB to 13.4 MB.
  It now bundles only `io/spine/tools/core`, JavaPoet, KotlinPoet, and Roaster.
  The consumer-resolved POM graph gained exactly the eleven ToolBase modules.
- 2026-10-29 — PR opened: SpineEventEngine/core-jvm-compiler#125.
- 2026-10-30 — moved to ToolBase `.440` (plain `protobuf-setup-plugins` and
  `jvm-tool-plugins`); dropped the uber-JAR ordering workaround, and restored
  `jvmToolPluginDogfooding` in the root build script.
