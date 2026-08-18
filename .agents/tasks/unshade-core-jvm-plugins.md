---
slug: unshade-core-jvm-plugins
branch: unshade-core-jvm-plugins
owner: claude
status: in-review
started: 2026-08-13
---

## Goal

`io.spine.tools:core-jvm-plugins` stops shipping ~198 third-party packages
unrelocated, so its copies no longer shadow the genuine artifacts on a consumer's
`buildscript` classpath. Success: `delivery-server` can bump `CoreJvmCompiler` past
`2.0.0-SNAPSHOT.081` and still run `./gradlew :delivery-server-cloud-run:jibBuildTar`.

## Context

Found from `delivery-server` (branch `bump-dependencies`) while copying the latest
`local` dependency objects from `config`. Reported by the `delivery-server` session;
nothing in this repo has been changed yet beyond this file.

### What regressed

`core-jvm-plugins` `.082` started fat-jarring its entire transitive dependency set
into the published JAR **without relocating any of it**.

|                                                 | `.081` |     `.082` |
|-------------------------------------------------|-------:|-----------:|
| total JAR entries                               | 12,977 | **35,634** |
| `org/apache/commons/compress/**` classes        |      0 |    **564** |
| `org/apache/commons/io/**` classes              |      0 |    **443** |
| newly bundled non-`io.spine` top-level packages |      — |   **~198** |

Among them: `org/apache/commons`, `com/google/gson`, `com/google/errorprone`,
`com/ibm/icu`, `com/thoughtworks/xstream`, `com/esotericsoftware/kryo`,
`com/twelvemonkeys/**`, `io/github/classgraph`, `com/jetbrains/rd`,
`dk/brics/automaton`, plus the `_COROUTINE` debug classes.

### Root cause — a content-derived shadow exclusion that silently inverted

`plugins/build.gradle.kts` (`tasks.shadowJar`, around line 455) computes its exclusion
set at execution time by **reading the entry names out of the `intellij-platform` and
`intellij-platform-java` JARs** and excluding everything found there (except Guava):

```kotlin
val ijPlatformJar = JarFile(intellijPlatform.files.single())
val ijPlatformJavaJar = JarFile(intellijPlatformJava.files.single())
val filesCombined = /* union of both JARs' entry names */
pathsToExclude = filesCombined.filter {
    !(it.contains("com/google/common") || it.contains("com/google/thirdparty"))
}.toSet()
```

The intent — "don't duplicate what the IntelliJ Platform artifacts already provide" —
is sound, but the rule keys off *what those JARs happen to contain* rather than off
declared coordinates. So:

- **ToolBase `.404`** — `intellij-platform` bundled commons-compress & co. inside
  itself → those entry names were in `filesCombined` → shadow **excluded** them →
  `core-jvm-plugins` `.081` ships 0 commons-compress classes.
- **ToolBase `.410`** — `intellij-platform` stopped bundling them and now *declares*
  them as ordinary POM dependencies → their entry names disappeared from
  `filesCombined` → nothing excluded them → shadow **swept them in** from the resolved
  runtime classpath, unrelocated → `core-jvm-plugins` `.082` ships 564 of them.

| artifact                   | bundled `org/apache/commons/compress/**` |
|----------------------------|-----------------------------------------:|
| `intellij-platform` `.404` |                                      564 |
| `intellij-platform` `.410` |                   **0** — fixed upstream |
| `core-jvm-plugins` `.081`  |                                        0 |
| `core-jvm-plugins` `.082`  |                      **564** — regressed |

The trigger is
[core-jvm-compiler#108 "Bump Compiler and ToolBase"](https://github.com/SpineEventEngine/core-jvm-compiler/pull/108)
(merged 2026-08-13 17:37 UTC, `b9ada6bd8`), which moves `ToolBase` `.404` → `.410`.
**That PR did not change any publishing or shadow configuration** — it is a dependency
bump. The exclusion logic inverted underneath it. In other words: fixing
`intellij-platform`'s unrelocated bundling is what broke the downstream fat JAR.

Corroboration inside the PR itself: its own dependency report shows
`core-jvm-annotation` `.081` → `.082` gaining a long list of new Runtime entries
(`be.cyberelf.nanoxml:nanoxml`, …), and `buildSrc/src/main/kotlin/module.gradle.kts`
gained forces for `commons-io`, `classgraph`, `objenesis`, `bcprov-jdk15on`, and
`commons-collections` — version conflicts surfacing from the same newly-visible
transitive set. The published-artifact consequence went unnoticed.

### Observed failure

```
> Task :delivery-server-cloud-run:jibBuildTar FAILED

com.google.cloud.tools.jib.plugins.common.BuildStepsExecutionException:
  'void org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
     .putArchiveEntry(org.apache.commons.compress.archivers.tar.TarArchiveEntry)'
```

The bundled copy predates commons-compress 1.26. `javap` against
`core-jvm-plugins-2.0.0-SNAPSHOT.082.jar`:

```
public void putArchiveEntry(org.apache.commons.compress.archivers.ArchiveEntry)
```

Genuine `org.apache.commons:commons-compress:1.26.0` has both overloads:

```
public void putArchiveEntry(org.apache.commons.compress.archivers.tar.TarArchiveEntry)
public void putArchiveEntry(org.apache.commons.compress.archivers.ArchiveEntry)
```

Jib is compiled against the 1.26 `TarArchiveEntry` overload, so it throws
`NoSuchMethodError` the moment the shaded class wins.

### Why consumers cannot work around it

`delivery-server` already carries a workaround for the older `intellij-platform`
case: it declares the genuine `commons-compress:1.26.0` **first** on the buildscript
classpath so it precedes the uber JAR.

That beats only a *transitively* pulled uber JAR. `core-jvm-plugins` is a **direct**
`classpath(...)` entry — present regardless of ordering — so the workaround has no
effect. `resolutionStrategy.force` does not help either: the resolved graph is already
correct at `1.26.0`; a duplicate class inside another JAR is invisible to dependency
resolution. The only consumer-side fix is pinning back to `.081`.

commons-compress is merely the collision that surfaced. The other ~197 bundled
packages are latent hazards for any consumer plugin sharing the classloader.

## Plan

- [x] Identify the root cause — content-derived `shadowJar` exclusion in
      `plugins/build.gradle.kts`, inverted by the `ToolBase` `.404` → `.410` bump in
      [#108](https://github.com/SpineEventEngine/core-jvm-compiler/pull/108)
- [x] Decide the fix — option 2 (coordinate-keyed exclusion), refined, plus the
      regression guard from the next-but-one item to cover the noted fragility:
      the shadow `DependencyFilter` excludes every module reachable in the
      *resolved runtime classpath graph* only through `intellij-platform` /
      `intellij-platform-java` (reachability-based, so shared modules such as
      Guava stay automatically), plus an explicit `runtimeProvidedModules` set
      (Kotlin runtime, KSP `symbol-processing-aa-embeddable`, JetBrains
      annotations) whose resources must not leak either. Option 1 (relocation)
      was not requested and would change runtime behaviour; option 3 rejected —
      the fat JAR stays
- [x] Implement in `plugins/build.gradle.kts`
- [x] Add a regression guard — `verifyBundledPackages` task fails the build if
      the fat JAR contains any `.class` (multi-release entries normalized)
      outside an explicit package allowlist; runs as `shadowJar` finalizer and
      from `check`
- [ ] Audit `io.spine.tools:compiler-cli-all` — it bundles
      `org/apache/commons/compress/**` unrelocated too (observed in `.062`, `.063`,
      `.065`; re-checked `.066`: still 524 compress classes, JNA trimmed to 125).
      Harmless for `delivery-server` (not on its buildscript classpath), but
      the same pattern. Belongs to the Spine Compiler repo, not this one
- [ ] Publish a snapshot and verify from `delivery-server`:
      `./gradlew :delivery-server-cloud-run:jibBuildTar`
- [ ] Unpin `CoreJvmCompiler` in `delivery-server` once the fix ships

## Verification

Bisect, with `CoreJvmCompiler.version` in `delivery-server` the only variable:

- `2.0.0-SNAPSHOT.082` → `jibBuildTar` FAILS (`NoSuchMethodError` above)
- `2.0.0-SNAPSHOT.081` → `BUILD SUCCESSFUL in 8s`

Inspect the shading directly:

```bash
jar tf core-jvm-plugins-2.0.0-SNAPSHOT.082.jar | grep -c '^org/apache/commons/compress/'
```

Note that `./gradlew build` in `delivery-server` passes on `.082` — it never invokes
Jib. `jibBuildTar` is the cheap check (no Docker needed, fails in ~20s).

## Log

- 2026-08-13 — drafted from the `delivery-server` investigation; not yet approved.
  `delivery-server` is keeping `.082` with `jibBuildTar` red pending this fix,
  rather than pinning back.
- 2026-08-13 — root cause confirmed by reading `tasks.shadowJar` in
  `plugins/build.gradle.kts`: the exclusion set is derived from the *contents* of the
  `intellij-platform` JARs, so it stopped excluding anything once those JARs stopped
  bundling. Initial suspicion that #108 changed publishing config was wrong — it is a
  pure dependency bump; the logic inverted underneath it.
- 2026-08-13 — implemented and verified locally (`:plugins:shadowJar` +
  `:plugins:verifyBundledPackages` green). Content vs `.081` baseline
  (multi-release-normalized class counts): `.081` = 12,000, `.082` = 28,730,
  fixed = 14,302; `org/apache/commons/compress/**` = 0. Only-new package roots
  vs `.081` are `tools/jackson` (Jackson 3) and `org/snakeyaml`
  (snakeyaml-engine) — legitimate ToolBase `.410` dependencies arriving outside
  the IntelliJ Platform graph. Intentionally dropped vs `.081`: 9 stray
  `com/sun/jna` classes (unusable fragment of `roaster-jdt`'s embedded JNA, now
  excluded fully), 2 `org/checkerframework` crumbs (came from
  `symbol-processing-aa-embeddable`, now excluded as a module), 2 multi-release
  `ksp` crumbs. Resource-level parity also restored: IJ descriptor XMLs,
  `misc/registry.properties`, Kotlin runtime `*.kotlin_module` / service files,
  `javax.xml.stream` ServiceLoader registrations, license/Maven/proguard
  metadata no longer leak.
- 2026-08-13 — upstream packaging smells found while tracing resources, for
  separate ToolBase follow-up: `protobuf-setup-plugins` `.410` embeds
  kotlin-reflect/stdlib `*.kotlin_module` and `kotlin.reflect.*` service files;
  `psi-java` `.410` embeds `misc/registry.properties`. Both are papered over
  here with path excludes.
- 2026-08-13 — `runtimeProvidedModules` now derives from the `buildSrc` dependency
  objects (`Kotlin.StdLib.modules`, `Kotlin.reflect`, `kotlinx.Coroutines.modules`,
  `JetBrainsAnnotations`, `Ksp.symbolProcessingAaEmb`) instead of string literals;
  the family-wide lists also dropped `kotlin-stdlib-common`'s metadata-only JAR
  (140 fewer resource entries, class content unchanged). Bonus fix confirmed:
  `.081` shipped `META-INF/services/` registrations pointing at classes it
  excluded (`ksp.*` relocations, `javax.annotation.processing.Processor` →
  `ksp.javaslang.match.PatternsProcessor`); these no longer leak.
- 2026-08-13 — `:plugins:check` green (TestKit tests against locally published
  `.083` artifacts), `spine-code-review` agent: APPROVE, no findings.
- 2026-08-13 — human review caught `val x by tasks.registering { }` — deprecated
  in Gradle 9, removed in 10. Fixed in `plugins/build.gradle.kts` and in the
  repo-owned `buildSrc/src/main/kotlin/module.gradle.kts`
  (`prepareProtocConfigVersions`); `--warning-mode all` now reports zero
  delegate deprecations. `detekt-code-analysis.gradle.kts:67` still uses the
  delegate form but is `config`-distributed — needs an upstream fix there.
  Team memory added: `no-gradle-kotlin-dsl-task-delegates`.
- 2026-08-13 — `gradle-review` agent: REQUEST CHANGES; all findings applied:
  `group = SpineTaskGroup.name` (was `"verification"`), no trailing period in
  the task description, marker output + `PathSensitivity.NONE` so
  `verifyBundledPackages` can be UP-TO-DATE, and the plain `OSGI-INF/**`
  exclude added next to its multi-release variant. Verified by two consecutive
  builds; the guard still re-runs today only because the module `jar` tasks
  are never up-to-date in this repo, which is pre-existing behaviour.
- 2026-08-13 — per review on PR #109: Jackson 3 (`tools.jackson`) and
  snakeyaml-engine moved out of the fat JAR too, so consumers can upgrade them
  independently. The five `tools.jackson` artifacts are now `runtime`
  dependencies in `pom.xml` (`pomProvidedModules` excludes them from the
  merge); snakeyaml-engine arrives transitively with `jackson-dataformat-yaml`.
  Census after the change: `tools/jackson` = 0, `org/snakeyaml` = 0, total
  classes 14,302 → 12,730; publication POM verified; `:plugins:check` green.
  Jackson 2 (`com/fasterxml`) and SnakeYAML (`org/yaml`) remain bundled as in
  `.081` — same treatment pending a separate decision.
- 2026-08-13 — decision made on PR #109: Jackson 2 and SnakeYAML unbundled the
  same way. `pom.xml` now declares the seven artifacts whose classes were
  bundled (`jackson-annotations` 2.22 shared by both lines; `jackson-core`,
  `jackson-databind`, `jackson-dataformat-yaml`, `jackson-datatype-guava`,
  `jackson-datatype-jdk8`, `jackson-module-parameter-names` 2.22.1);
  `org.yaml:snakeyaml` comes transitively with `jackson-dataformat-yaml`, like
  snakeyaml-engine. `jackson-jr-objects` and `jackson-module-kotlin` 2.x are
  IntelliJ-Platform-only on this classpath, so they get no POM entries. Census:
  `com/fasterxml` = 0, `org/yaml` = 0, total classes 12,730 → 11,271; remaining
  roots: `io/spine`, Guava (+`auto`, `gradle`), Aedile, JavaPoet/KotlinPoet,
  `kotlinx` datetime/atomicfu, `kr/motd`, animal-sniffer, Roaster.
  `:plugins:test --rerun` green against the republished artifacts.
- 2026-08-13 — remaining: publish a snapshot (CI on merge) and re-run
  `./gradlew :delivery-server-cloud-run:jibBuildTar` in `delivery-server`, then
  unpin. Not attempted from this session: `delivery-server` is being worked on
  by a parallel session; local cross-repo verification would require editing
  its working tree.
