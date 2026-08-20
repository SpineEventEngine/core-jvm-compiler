---
name: projectbuilder-in-memory-caches
description: ProjectBuilder stub projects can never reuse the Gradle dependency cache — TestInMemoryCacheFactory replaces persistent caches; serve stub deps from the local stub repo and keep stubs offline.
metadata:
  type: project
  since: 2026-08-19
---

Dependency resolution inside a `ProjectBuilder`-created project cannot hit
`~/.gradle/caches`, no matter what `withGradleUserHomeDir(...)` is set to.
`ProjectBuilderImpl` wires its global services through
`TestGlobalScopeServices`, which overrides `createCacheFactory` to return
`TestInMemoryCacheFactory` — every "persistent" cache, including the module
metadata store, is an empty in-memory map in each test JVM. Symptoms: the
resolver id, user home, and on-disk metadata all look correct, yet offline
resolution reports "No cached version available", and online resolution
re-fetches the whole graph on every run. Under Maven Central consumption
limits (HTTP 429 per egress, since May 2026), that traffic gets the whole
machine blocked.

**Why:** diagnosed 2026-08-19 when `CoreJvmOptionsSpec` and friends failed
with 429s: sharing the user home changed nothing; reading Gradle v9.7.1
sources (`platforms/extensibility/unit-test-fixtures/.../ProjectBuilderImpl.java`,
`TestGlobalScopeServices.java`) revealed the in-memory cache substitution.

**How to apply:** stub projects must resolve from local repositories only.
The pattern (see `StubResolution.kt` in `base` test fixtures and
`prepareStubRepo` in `gradle-plugin/build.gradle.kts`):

1. `Project.forbidNetworkResolution()` sets `startParameter.isOffline`,
   so any gap fails loudly with "No cached version available for offline
   mode" instead of hitting the network.
2. The test-hosting module's `stubRepoDeps` configuration lists the
   third-party artifacts which plugins inject into consumer projects
   (e.g., `auto-service-annotations` by `CommonKspSettingsPlugin`,
   `spine-time` by the Time plugin); `prepareStubRepo` copies them from
   the enclosing build's cache into `build/stub-repo` (Maven layout,
   no POMs).
3. Test-side `StandardRepos.applyStandard()` serves that directory as
   a `maven { metadataSources { artifact() } }` repository before
   `mavenLocal()`. POM-less metadata means transitives do not expand —
   fine for specs that assert settings, not classpath contents.
4. When a spec fails with "No cached version …", add the named artifact
   to `stubRepoDeps` — do not widen repositories or drop offline.

TestKit-based ig tests are unaffected (real Gradle, real persistent
caches), but any resolution that *consults* Maven Central fails while
a 429 block is active, even when an earlier repository has the module —
a repo error aborts resolution. Do not poll Central to "wait out" a block:
requests during a block extend it. Related: [[igtest-stale-plugins]].
