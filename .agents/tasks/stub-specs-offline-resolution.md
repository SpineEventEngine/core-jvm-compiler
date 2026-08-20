---
slug: stub-specs-offline-resolution
branch: split-plugins-module
owner: claude
status: in-review
started: 2026-08-19
pr: https://github.com/SpineEventEngine/core-jvm-compiler/pull/111
related-memories:
  - projectbuilder-in-memory-caches
---

## Goal

Stop the `ProjectBuilder`-based specs of the `gradle-plugin` module from
downloading their dependency graphs over the network on every run — the
pattern that trips Maven Central consumption limits and gets the whole
machine blocked with HTTP 429.

## Context

- Root cause (proven against Gradle v9.7.1 sources): `ProjectBuilder`
  substitutes `TestInMemoryCacheFactory` for the persistent caches
  (`TestGlobalScopeServices.createCacheFactory`), so stub projects can
  never reuse `~/.gradle/caches` — pointing `withGradleUserHomeDir` at
  the real home changes nothing. See
  [[projectbuilder-in-memory-caches]] for the full story.
- Verified intermediate dead end: sharing the user home left resolver id,
  home dir, and on-disk metadata all correct, while offline resolution
  still reported "No cached version available".

## Plan

- [x] `base` test fixtures: `StubResolution.kt` —
      `Project.forbidNetworkResolution()` (offline resolution) and
      `stubRepository` (path from the `stub.repository` system property).
- [x] `gradle-plugin/build.gradle.kts`: `stubRepoDeps` configuration
      (auto-service-annotations, spine-time ×2, spine-server/client,
      validation runtime) + `prepareStubRepo` task copying the artifacts
      into `build/stub-repo` (Maven layout, POM-less); `test` wires the
      task and the system property.
- [x] Test-side `StandardRepos.applyStandard()`: serve `build/stub-repo`
      first, with `metadataSources { artifact() }`.
- [x] `StubProject.createAt` and `CoreJvmOptionsSpec` call
      `forbidNetworkResolution()`.
- [x] `CoreJvmPluginIgTest.settingsWithRepositories`: add the Spine
      registry repositories to `pluginManagement` — the nested build's
      plugin graph (validation, time, compiler artifacts) lives there;
      the old template only worked when `~/.m2` happened to hold them.
- [x] Verify: all previously failing unit specs (17) pass fully offline
      while the Maven Central 429 block is still active — the strongest
      hermeticity proof available.
- [x] Re-run the full suite once the 429 block lifts: confirmed on
      2026-08-20 — Maven Central answers 200 again and
      `:gradle-plugin:test` is fully green (27 tests, 0 failures),
      including `CoreJvmPluginIgTest > be available via its ID and
      version`, whose nested TestKit build had been aborted by
      the repository error during marker lookup.

## Log

- 2026-08-19 22:40 — implemented and verified; 27/28 tests green under an
  active Central block; the last one is blocked by the environment, not
  by code. Team memory written.
