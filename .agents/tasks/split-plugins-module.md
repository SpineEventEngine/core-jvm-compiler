---
slug: split-plugins-module
branch: split-plugins-module
owner: claude
status: in-progress
started: 2026-08-19
related-memories:
  - igtest-stale-plugins
---

## Goal

Split the `plugins` module into two modules:

- `gradle-plugin` — a regular (thin) JAR with the code that configures
  a Gradle project to which the CoreJvm Gradle Plugin is applied
  (`CoreJvmPlugin`, `CleaningPlugin`, `CompilerConfigPlugin`,
  `WriteCompilerPluginsSettings`, `Meta`). Publishes as
  `io.spine.tools:core-jvm-gradle-plugin` and owns the `io.spine.core-jvm`
  plugin declaration, the Plugin Portal publishing, and the marker POMs.
- `compiler-plugins` — the renamed `plugins` module keeping the fat JAR
  (`shadowJar`) which bundles the Spine Compiler plugins from the feature
  modules. Still publishes as `io.spine.tools:core-jvm-plugins` so that
  the artifact name visible to consumers does not change.

## Context

- The old fat JAR served two classpaths at once: the consumer's *build*
  classpath (Gradle-plugin side) and the Spine Compiler *user* classpath
  (code-generation plugins, injected via `addUserClasspathDependency`).
- Feature modules (`base`, `annotation`, …) are **not** published
  individually (`projectsToPublish()` = `modulesWithCustomPublishing`
  only), so the thin JAR's POM cannot reference them. Instead it declares
  a single `runtime` dependency on the fat JAR; all curated runtime
  dependencies of the fat JAR POM (`tuneDependencies()`) flow to
  consumers transitively, keeping the consumer build classpath identical.
- `CustomPublicationHandler.copyProjectAttributes()` replaces a
  publication's `artifactId` only when it starts with the project name —
  after the rename the fat JAR publication must set
  `artifactId = CoreJvmCompiler.fatJarArtifact` explicitly, or it would
  silently publish as `core-jvm-compiler-plugins`.
- The artifact-meta resource moves with the reader: `gradle-plugin`
  records its own artifact (`core-jvm-gradle-plugin`) and the versions of
  KGP / Protobuf GP / KSP GP / Validation. `Meta` now extends
  `LazyMeta(CoreJvmCompiler.gradlePlugin)`, and `CompilerConfigPlugin`
  injects the fat JAR as `CoreJvmCompiler.fatJar(Meta.artifact.version)`
  (both artifacts always share one version).
- TestKit/integration builds that used to put the fat JAR on the
  buildscript classpath must switch to the thin artifact (the descriptor
  `META-INF/gradle-plugins/io.spine.core-jvm.properties` now lives there).
- The dogfooding classpath (`CoreJvmCompiler.pluginLib`,
  version `2.0.0-SNAPSHOT.082`) predates the split, so it keeps pointing
  at the fat JAR until `dogfoodingVersion` is bumped past the first
  post-split release.

## Plan

- [x] `git mv plugins compiler-plugins`; `git mv compiler-plugins/src
      gradle-plugin/src` (all sources are Gradle-plugin-side).
- [x] New `gradle-plugin/build.gradle.kts`: module conventions,
      `artifact-meta` (own artifact + recorded tool versions), same
      dependency declarations as before, custom publication `pluginJar`
      (`tasks.jar` + POM `runtime` dep on the fat JAR), `plugin-publish`,
      `gradlePlugin` block, snapshot gate for `publishPlugins`,
      `pluginMaven` removal + marker POM fix, `sourcesJar` wiring,
      `javadoc.enabled = false`, `test dependsOn localPublish`.
- [x] Rewrite `compiler-plugins/build.gradle.kts`: keep shadow JAR
      config, `tuneDependencies()`, exclusion sets,
      `verifyBundledPackages`; publication `fatJar` with explicit
      `artifactId = core-jvm-plugins`; drop plugin-publish, artifact-meta,
      gradlePlugin block, marker hack, and test wiring.
- [x] `settings.gradle.kts`: `plugins` → `compiler-plugins`,
      `gradle-plugin`.
- [x] Root `build.gradle.kts`: `modulesWithCustomPublishing =
      setOf("compiler-plugins", "gradle-plugin")`; fix `:plugins` comment.
- [x] `base` module `Meta.kt`: add `CoreJvmCompiler.gradlePlugin` module
      constant.
- [x] `gradle-plugin` sources: `Meta` targets the thin module;
      `CompilerConfigPlugin` injects `CoreJvmCompiler.fatJar(version)`;
      `CoreJvmPluginIgTest` classpath uses `Meta.artifact.coordinates`.
- [x] ~~buildSrc `CoreJvmCompiler.kt`: add `gradlePluginArtifact`,
      `gradlePluginLib()`, `fatJarLib()`~~ — **reverted**: that file is
      distributed by the `config` submodule, so `./config/pull` deletes
      the additions and breaks build-script compilation on every machine.
      The artifact ID is now declared locally in
      `gradle-plugin/build.gradle.kts` (the convention `grpc` and `base`
      follow), and the consumer build scripts use literal coordinates.
      See [[config-owned-buildsrc-reverts]].
- [ ] Follow-up in the `config` repo: add the `core-jvm-gradle-plugin`
      artifact to the shared `CoreJvmCompiler` object, so that other
      Spine repositories can consume the new plugin artifact by name.
      Only then switch `dogfoodingVersion`-based `pluginLib` over to it.
- [x] Migrate consumers: `tests/build.gradle.kts`,
      `annotation/src/test/resources/annotator-plugin-test`,
      `signal/src/test/resources/rejection-codegen-test`,
      `signal/src/test/resources/rejection-javadoc-test` →
      `gradlePluginLib`; `tests/factories` → `fatJarLib` (it needs the
      bundled `base` classes, not the plugin).
- [x] Docs: `docs/project.md` module list;
      `.agents/memory/project/igtest-stale-plugins.md` task path.
- [ ] Full build (`JAVA_HOME` = JDK 17, `LC_ALL=C.UTF-8`, output to a log
      file), including integration tests.
- [ ] Self-review (gradle-review / spine-code-review agents on the diff).

## Log

- 2026-08-19 21:30 — plan drafted; reconnaissance complete, executing.
- 2026-08-19 21:35 — implementation done; compile, `localPublish`,
  `verifyBundledPackages`, artifact/POM layout, and Dokka all verified.
  `:gradle-plugin:test` blocked by Maven Central HTTP 429 rate limiting
  (stub-project specs resolve dependencies over the network by design).
- 2026-08-19 22:05 — user's full `build` failed: with `plugin-publish` gone
  from the fat-JAR module, nothing reclassifies the plain `jar` to `-main`
  anymore (plugin-publish's Shadow support did that), so `jar` and
  `shadowJar` (empty classifier) collided on one archive path — Gradle 9
  implicit-dependency validation. Fixed by disabling `tasks.jar` in
  `compiler-plugins`, mirroring the `uber-jar-module.gradle.kts`
  convention. Verified: `jar` SKIPPED, `assemble` + `localPublish` +
  `verifyBundledPackages` green; marker POM unchanged. Also applied
  `gradle-review` finding: hoisted `project.version` out of `pom.withXml`
  closures (configuration-cache hazard) and added drift-guard comments to
  the mirrored dependency lists. Reviews: `spine-code-review` APPROVE,
  `gradle-review` APPROVE WITH CHANGES (all applied).
- 2026-08-20 — replaced the hand-maintained `cliProvidedModules` set with
  a content-derived `providedByCli` predicate inspecting the actual
  `compiler-cli-all` JAR (package-level containment, tool-base style),
  plus a 14-module `bundledDespiteCli` keep-list for what the Gradle-plugin
  side needs on consumers' build classpath. The analysis found the old
  hand list had a silently ineffective entry: Guava was "excluded" there,
  but its classes ride embedded in `protobuf-setup-plugins` regardless.
  Verified: rebuilt fat JAR classes byte-identical; 7 dangling resource
  entries (grpc/palantir service files, native-image configs) dropped;
  `verifyBundledPackages` and `CoreJvmPluginIgTest` green.
- 2026-08-20 — reduced `bundledDespiteCli` to the four ToolBase modules
  (the ten `io.spine`-group entries reach consumers as Maven transitives
  of the POM-declared Compiler artifacts); applied the `gradle-review`
  findings (`configurations.create`, `ConcurrentHashMap.computeIfAbsent`,
  shared `classFileNamesOf()`, KDoc precision); moved `:grpc` — a module
  with no Compiler plugin — out of the fat JAR and into the JAR of
  `gradle-plugin`. Each step gated on the full root build including
  `integrationTests`; all green.
- 2026-08-20 — relocated `:routing` (with `:ksp`) the same way: no Compiler
  plugins there; `RoutingPlugin` now points KSP at the plugin artifact,
  and the plugin POM declares `kotlinpoet-ksp` as a `runtime` dependency
  (committed as `7443c9167` together with the two items above).
- 2026-08-20 — the strings migration: `CompilerConfigPlugin` and
  `WriteCompilerPluginsSettings` refer to the code-generation plugins via
  the `CoreJvmCompilerPlugins` name constants, dropping the compile-time
  dependencies on `:entity`, `:signal`, `:marker`, `:message-group`,
  `:uuid`, and `:comparable` (now test-only, serving the drift-guard
  `CoreJvmCompilerPluginsSpec`). `:annotation` remains for its settings
  proto DSL — the only settings proto living outside `base`; moving it
  to `base` is the remaining step. Full gate green.
