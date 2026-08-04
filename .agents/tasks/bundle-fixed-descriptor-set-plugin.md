---
slug: bundle-fixed-descriptor-set-plugin
branch: bundle-fixed-descriptor-plugin   # suggested; the executing session creates it
owner: claude
status: draft
started: 2026-07-17
---

## Goal

Make the `core-jvm-plugins` fat JAR ship the **fixed** `DescriptorSetFilePlugin`
from `tool-base` so that a downstream project bumping its version no longer gets
a dangling `desc.ref` (an `UnknownTypeException` at runtime) when the Gradle
build cache is enabled.

Success = a newly published `core-jvm-plugins` (e.g. `2.0.0-SNAPSHOT.081`)
whose bundled `io.spine.tools.protobuf.gradle.plugin.DescriptorSetFilePlugin`
declares the `descriptorSetName` cache-key input, and a downstream consumer
(delivery-server) building green **without** the manual
`inputs.property("projectVersion", …)` workaround.

## Context

### The bug, downstream

delivery-server PR #58 (https://github.com/SpineEventEngine/delivery-server/pull/58)
hit an intermittent CI failure:

```
io.spine.type.UnknownTypeException: No Java class found for the Protobuf message
of type: `spine.delivery.TestEvent`.
```

Root cause of the *runtime* symptom: the generated descriptor set file embeds the
project version in its **path** (`…_spine-simple-server_<version>_test.desc`),
while `desc.ref` (fixed path) carries the version only in its **contents**. With
`org.gradle.caching=true` and a cache persisted across CI runs, a build at a new
version restored the descriptor bytes under the new path but restored `desc.ref`
contents naming the **old** version → the reference dangles → `KnownTypes` skips
all of the project's own descriptors → packing a project type fails.

### Why this is a `core-jvm-compiler` task, not a `tool-base` one

`tool-base` **already fixed this** in the plugin itself:

- commit `c914abec` — declared the project version as a task input;
- commit `502e3dfe` — superseded it by keying on the descriptor set **file name**
  (covers version, group, artifact ID, and classifier at once, via the
  `descriptorSetName` input property).

Those landed in `tool-base 2.0.0-SNAPSHOT.402` and are present in `.403`. The
`DescriptorSetFilePlugin` on `master` is correct; **no `tool-base` change is
needed**.

The problem is that delivery-server never *ran* the fixed plugin. Its buildscript
classpath carried **two** copies of
`io.spine.tools.protobuf.gradle.plugin.DescriptorSetFilePlugin`:

1. the **fixed** `protobuf-setup-plugins:.403`, pulled transitively via
   `Compiler.pluginLib` (Compiler `.061` pins `tool-base .403`);
2. a **stale** copy baked into the `core-jvm-plugins` fat JAR.

The stale copy won classpath ordering, so the pre-fix behavior (`desc.ref`
declared as an output but the descriptor **name** absent from the cache key —
the `.400`/`.401` signature) is what executed. A CI diagnostic on the PR
confirmed exactly that signature.

### Why the fat JAR is stale (the mechanism to fix here)

In this repo, `:plugins` builds the `core-jvm-plugins` shadow JAR and **bundles
`protobuf-setup-plugins` unrelocated**:

- proof it's inside the JAR: the exclude at `plugins/build.gradle.kts:515`
  (`META-INF/io.spine/io.spine.tools_protobuf-setup-plugins.meta`);
- there is **no `relocate` call** — the classes ship under their original
  `io.spine.tools.protobuf.gradle.plugin.*` package.

The bundled version is **forced to `ToolBase.version`**. The `:plugins` module —
which produces the fat JAR — applies the `module` convention plugin, whose
`forceConfigurations()` resolution strategy forces `protobuf-setup-plugins` to
`ToolBase.version` for every subproject configuration, including the
`shadowRuntimeElements` classpath that `shadowJar` bundles:

- `buildSrc/src/main/kotlin/module.gradle.kts:184` — `ToolBase.protobufSetupPlugins`
  in the `force(...)` list (i.e. `protobuf-setup-plugins:${ToolBase.version}`),
  present since `6cc97740f` (2025-10-21) **and already on `master`**.

So the bundled plugin is **not** selected transitively through
`Compiler.pluginLib`; it is pinned to `ToolBase.version`. On `master` that value
is `.401`, which is why the fat JAR carries the pre-fix plugin:

| pin (on `master`, `2.0.0-SNAPSHOT.080`)                                             | value  | consequence                                                                |
|-------------------------------------------------------------------------------------|--------|----------------------------------------------------------------------------|
| `ToolBase.version` / `dogfoodingVersion` (`buildSrc/.../local/ToolBase.kt:37-38`)   | `.401` | **forces the bundled `protobuf-setup-plugins` to the pre-fix `.401`**       |
| `Compiler.fallbackVersion` / `fallbackDfVersion` (`buildSrc/.../local/Compiler.kt`) | `.054` | pulls `protobuf-setup-plugins .400` transitively — overridden by the force  |

Because the bundle tracks `ToolBase.version`, **bumping `ToolBase` → `.403`
(step 1) is the decisive lever** that swaps the bundled plugin to the fixed one.
Bumping `Compiler` → `.062` (step 2) keeps the directly-referenced `tool-base`
surface coherent at `.403` and prevents intra-JAR skew, but does not by itself
control the bundled `protobuf-setup-plugins`. The root `build.gradle.kts`
`buildscript {}` `force(...)` block does not pin `protobuf-setup-plugins`, but
that block governs the build's own plugin classpath — not the published fat JAR.

Note the `.054` value is itself a deliberate `.055 → .054` rollback
(`d8ddbc406`, empty commit body — reason not recorded). Re-raising the Compiler
pin is the main risk in this task; see **Risks**.

## Plan

- [ ] **0. Re-confirm current state.** Pins may have moved since this doc was
  written (2026-07-17). Read `buildSrc/src/main/kotlin/io/spine/dependency/local/ToolBase.kt`
  and `…/local/Compiler.kt` and `version.gradle.kts`. Confirm `tool-base .403`
  is the intended fixed target (it contains `502e3dfe`), and find the latest
  **published** `Compiler` version built against `tool-base .403`
  (`.061` and `.062` both qualify; Compiler `master` is at `.062`).

- [ ] **1. Bump `ToolBase` → `.403`.** In `buildSrc/.../local/ToolBase.kt`, set
  both `const val version` and `const val dogfoodingVersion` to
  `2.0.0-SNAPSHOT.403`.

- [ ] **2. Bump `Compiler` → `.062`.** In `buildSrc/.../local/Compiler.kt`, set
  both `fallbackVersion` and `fallbackDfVersion` to `2.0.0-SNAPSHOT.062` (or the
  latest published value confirmed in step 0). This is the change that swaps the
  **bundled** `protobuf-setup-plugins` to the fixed `.403` and keeps the whole
  bundled `tool-base` surface (`plugin-base`, `jvm-tools`, …) coherent at one
  version — avoiding intra-JAR binary skew.

- [x] **3. Pin the bundled plugin explicitly (hardening) — already in place.**
  The fat JAR's descriptor-plugin version already tracks `ToolBase.version`
  deterministically: `buildSrc/src/main/kotlin/module.gradle.kts:184` forces
  `toolBase.protobufSetupPlugins` for every `:plugins` configuration, so a future
  Compiler rollback can never silently reintroduce a stale plugin. No edit is
  needed. (The earlier draft targeted the root `build.gradle.kts`
  `buildscript {}` force block, which governs the build's own plugin classpath,
  not the published fat JAR — that would have been a no-op.) Verified with
  `./gradlew :plugins:dependencyInsight --configuration runtimeClasspath
  --dependency io.spine.tools:protobuf-setup-plugins`, which reports
  `protobuf-setup-plugins:2.0.0-SNAPSHOT.403 (forced)`.

- [ ] **4. Bump this repo's own version.** Run the `bump-version` skill so
  `coreJvmCompilerVersion` in `version.gradle.kts` moves above `.080` (expected
  `.081`). Required: this is a new publish that downstream will pin to.

- [ ] **5. Build green.** `./gradlew build` (JDK 17). Must pass — this is the
  guard on the Compiler `.054 → .062` bump.

- [ ] **6. Update dependency reports** per repo convention, in a **separate**
  commit from the code change.

- [ ] **7. Open the PR.** On merge, `core-jvm-plugins .081` publishes with the
  fixed plugin bundled.

## Verification

1. **Fat-JAR sanity check (fast, local).** After step 5, confirm the shipped
   plugin is the fixed one. This inspects **this repo's own build output**
   (`plugins/build/libs/…`), not a `~/.gradle/caches` library, so the
   `api-discovery` "don't unzip JARs" rule does not apply:

   ```
   unzip -p plugins/build/libs/core-jvm-plugins-*.jar \
     io/spine/tools/protobuf/gradle/plugin/DescriptorSetFilePlugin.class \
     | strings | grep -E 'descriptorSetName|projectVersion'
   ```

   - **Fixed** (`.403`): prints `descriptorSetName`.
   - **Stale** (`.401`): prints neither. If you see neither, the bundle is still
     stale — recheck step 1 (`ToolBase.version`), which the bundle is forced to
     via `module.gradle.kts:184`.

2. **End-to-end (authoritative, downstream session — see Follow-up).** With
   delivery-server on the new `.081` fat JAR and the manual workaround removed,
   reproduce the cross-version cache reuse: build at version N, then at N+1 with
   `--build-cache`; assert `desc.ref` contents and the `*.desc` filename agree
   and the project's types resolve. Green = fix delivered through the whole chain.

## Risks

- **Compiler `.054 → .062` may reintroduce the reason for the historical
  `.055 → .054` rollback** (`d8ddbc406`; rationale not recorded). This is the
  primary risk. If `./gradlew build` breaks on the bump, capture the failure and
  treat the Compiler bump as an independent decision — it may need a Compiler-side
  fix first. Do **not** paper over it by reverting to `.054` while forcing
  `protobuf-setup-plugins:.403` alone: that would bundle a `.403` plugin
  alongside `.054`-era `tool-base` classes and risk `NoSuchMethodError` at
  runtime (intra-JAR skew). The whole bundled `tool-base` surface must move
  together.

- **Partial version alignment.** Ensure `ToolBase` (step 1) and `Compiler`
  (step 2) land together; a half-bump leaves the JAR internally inconsistent.

## Follow-up (separate session, `delivery-server`)

Once `core-jvm-plugins .081` is published:

1. Bump `CoreJvmCompiler` `.080 → .081` in delivery-server's
   `buildSrc/.../local/CoreJvmCompiler.kt` (its `Compiler` fallback is already
   `.061`, which carries the fix).
2. **Remove** the manual `inputs.property("projectVersion", …)` block added to
   delivery-server's root `build.gradle.kts` in PR #58 — with the fixed plugin
   now bundled, the workaround is redundant and would otherwise mask a future
   regression.
3. Verify CI green with the build cache on.

## Log

- 2026-07-17 — drafted from a tool-base session after validating that
  `DescriptorSetFilePlugin` on `master` is already correct and the defect is a
  stale plugin bundled in this repo's `core-jvm-plugins` fat JAR. Awaiting
  approval in a `core-jvm-compiler` session.
- 2026-07-17 — validated in a `core-jvm-compiler` session. **Claim confirmed:**
  `master`'s published `core-jvm-plugins 2.0.0-SNAPSHOT.080` fat JAR bundles the
  `DescriptorSetFilePlugin` *unrelocated* and *byte-identical* to the pre-fix
  `protobuf-setup-plugins .401` (SHA `2294713b…`; no `descriptorSetName` input).
  The fix is present in the published `.402` **and** `.403` artifacts and absent
  in `.401` — so the "landed in `.402`" note holds: the `.402` snapshot was
  re-published after PR #184 merged, even though the `.402` *bump commit*
  (`66c4fab2`) topologically predates the fix commits (`c914abec`, `502e3dfe`).
  **Mechanism corrected** (see the section above): the bundled version is
  *forced* to `ToolBase.version` via `module.gradle.kts:184` — on `master` since
  2025-10-21 — not selected transitively through `Compiler`. So step 3's guard
  already exists, and the `ToolBase → .403` bump (step 1) is the decisive lever.
  Branch `bump-tool-base-and-all` already carries steps 1 (`ToolBase .403`),
  2 (`Compiler .062`), and 4 (`version .081`); `:plugins:dependencyInsight`
  reports the bundle as `protobuf-setup-plugins:2.0.0-SNAPSHOT.403 (forced)`.
