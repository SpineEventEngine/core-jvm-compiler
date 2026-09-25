---
slug: sbom-publication-content
branch: publish-sboms
owner: claude
status: in-review
started: 2026-09-25
related-memories: []
---

## Goal

The SBOMs of #115 describe what the two artifacts publish and contain, with both POMs and the
thin JAR byte-identical:

- `core-jvm-gradle-plugin` `DEPENDS_ON` `core-jvm-plugins` and `kotlinpoet-ksp`, and `CONTAINS`
  `grpc`, `ksp`, and `routing`;
- `core-jvm-plugins` `DEPENDS_ON` its POM dependencies, and `CONTAINS` what Shadow packs.

## Context

- SpineEventEngine/config#768 (filed from #115), fixed by SpineEventEngine/config#769, pulled
  (`config` at `ca7def74`). A publication describes its SBOM with
  `sbom { dependencies(configuration); bundled(configuration | tasks.shadowJar) }`.
- `bundled(tasks.shadowJar)` sees only Shadow's dependency filter. So the five Gradle plugins
  the fat JAR's POM declares, stripped only by path today, must be excluded by the filter.
- `:compiler-plugins` disables its plain `jar`. A dependency on it must request
  `Bundling.SHADOWED`, to get `shadowRuntimeElements`.
- The `io.spine.artifact-meta` plugin records every configuration of `gradle-plugin` into its
  runtime `.meta` resource. The new configurations must be excluded from it.
- Plan approved on 2026-09-25: `.claude/plans/jolly-frolicking-cascade.md`.

## Plan

- [x] 0. Baselines: both POMs, both JARs (entry name and CRC), and
      `:compiler-plugins:outgoingVariants`.
- [x] 1. Commit "Update `config`": the #769 `buildSrc` and the `config` pointer.
- [x] 2. `PluginMarkerPomSpec` review items; one `val` for the plugin declaration name.
- [x] 3. The `bundledModules` configuration drives `tasks.jar` and is excluded from
      `artifactMeta`. The thin JAR must stay identical.
- [x] 4. The POM entries as `ExternalModuleDependency` lists; `tuneDependencies()` writes the
      POM from them. Both POMs must stay byte-identical.
- [x] 5. The `fatJarPom` and `pluginJarPom` configurations, `sbom { }`, and the POM part of
      `pomProvidedModules` derived from the list. The fat JAR may lose only six known stale
      entries.
- [x] 6. "Update dependency reports".
- [ ] 7. `/pre-pr`, push, update #115's description; this file becomes `done`.
- [x] 8. File a `config` issue: since #764, POMs carrying an SBOM get `<packaging>pom</packaging>`.

## Log

- 2026-09-25 — drafted after pulling `config` `ca7def74` (#769).
- 2026-09-25 — reviewed by a Plan agent, which added the script-order, `artifact-meta`,
  `SHADOWED`, filter-by-construction, and CRC-check points, and found the packaging regression.
  The user asked for `bundledModules` as the name, and Gradle's `ExternalModuleDependency` over
  an ad-hoc data shape. Approved.
- 2026-09-25 — steps 0–5 done: `b636da09ff`, `4623bfbd13`, `3ae2633ded`, `c52f5d3867`
  (the `bundledModules` deps moved into the main block, per the user), `4f8b1ae1c8`, and
  `44308c6a57`. Both POMs and the thin JAR are byte-identical; the fat JAR lost exactly
  the six predicted stale entries; `verifyBundledPackages` passes. The SBOMs match
  the Goal, and #768's script reports only the POM-only KSP marker.
- 2026-09-25 — step 6: `bf04ba0e26`. The reports now also list the dependencies the fat
  JAR's POM declares. Step 8: filed SpineEventEngine/config#770. Step 7 (`/pre-pr`, the
  push, and #115's description) follows this commit. Delete this file on merge.
