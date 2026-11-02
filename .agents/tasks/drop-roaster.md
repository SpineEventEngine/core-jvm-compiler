---
slug: drop-roaster
branch: claude/drop-roaster-968ba6
owner: claude
status: in-review
started: 2026-11-02
---

## Goal

CoreJvm Compiler no longer uses or bundles Roaster. Tests that read generated Java
sources do so through PSI, and the `core-jvm-plugins` fat JAR carries no Roaster
or JNA classes.

## Context

- The Spine SDK is dropping the classic Java codegen libraries, JavaPoet and
  Roaster. Roaster goes first. Consumers change before ToolBase, so that
  upgrading to a ToolBase release without Roaster breaks nothing here.
- No production code here imports Roaster. Tests in `signal` and the test
  fixtures of `annotation` parse generated Java with it.
- Roaster reaches the fat JAR through `ToolBase.classicCodegen`, which declares
  `roaster-api` and `roaster-jdt` as `api`. `roaster-jdt` embeds JNA.
  In ToolBase, only `RoasterExts.kt` uses Roaster, and nothing here calls it.

## Plan

- [x] `signal`: read rejection Javadoc in `RejectionJavadocIgTest` with PSI;
      drop `ToolBase.classicCodegen` from the test dependencies
- [x] `annotation`: move the `check` test fixtures and `ApiAnnotationsPluginIgTest`
      to PSI; drop `Roaster.api`/`Roaster.jdt` from `testFixturesImplementation`
- [x] Exclude the Roaster group from `ToolBase.classicCodegen` in `base`,
      `routing`, and `signal` until ToolBase drops it
- [x] Drop `Roaster.api`/`Roaster.jdt` from the `force(...)` list in
      `buildSrc/src/main/kotlin/module.gradle.kts`
- [x] `compiler-plugins`: drop `org/jboss/forge/` from the expected packages and
      the `com/sun/jna/**` exclusion
- [x] Verify: `./gradlew build`, the IgTests, the shadow JAR listing
- [x] After upgrading to a ToolBase release without Roaster: drop the
      `Roaster.group` exclusions in `base`, `routing`, and `signal`. The one in
      `compiler-plugins` goes with the `classic-codegen` POM entry, once JavaPoet is gone.
- [x] Upgrade to ToolBase `.442`, whose `Parser.parse()` normalizes line separators,
      and drop the `convertLineSeparators()` calls in the two IgTests

## Log

- 2026-11-02 — drafted, executing
- 2026-11-02 — `./gradlew build` green at `2.0.0-SNAPSHOT.103` (working tree, uncommitted),
  incl. `integrationTests`; `RejectionJavadocIgTest` 5/5, `ApiAnnotationsPluginIgTest` 18/18.
  Fat JAR 13.4 MB -> 2.6 MB, 4,663 -> 908 files; no `org/jboss/`, `com/sun/jna/`, or
  Roaster service entries; every other dropped entry belongs to `roaster-jdt`.
