---
name: buildspeed-kgp-version-skew
description: BuildSpeed pins its own Kotlin; when Spine Gradle plugins are built against a newer KGP, checkPerformance fails with NoSuchMethodError on a KGP API — bump BuildSpeed's kotlinVersion to match.
metadata:
  type: project
  since: 2026-08-13
---

The `checkPerformance` task (root `build.gradle.kts`) substitutes the current
`Compiler`/`CoreJvmCompiler` versions into `BuildSpeed/settings.gradle.kts`,
publishes this repo to Maven Local, and runs a child build of `BuildSpeed`.
The child therefore executes the *current* Spine Gradle plugins under
BuildSpeed's *own* Kotlin Gradle Plugin, pinned as `kotlinVersion` in
`BuildSpeed/buildSrc/build.gradle.kts`.

When the Spine plugin stack moves to a newer KGP, the child build fails at
configuration time with a `NoSuchMethodError` on a KGP API. Occurred at least
twice; the 2026-08-13 instance was
`KotlinSourceSet.getGeneratedKotlin()` (added in KGP 2.3.x, called by
tool-base's `GeneratedSourcePlugin` via the Compiler's `configureProtoTask`)
under BuildSpeed's KGP 2.2.20.

**Why:** a Gradle plugin compiled against a newer KGP may call APIs absent
from the older KGP loaded by the consuming build; the failure surfaces only
in `checkPerformance`, because the regular CI builds use this repo's own
(current) KGP.

**How to apply:** on such a `NoSuchMethodError` in the "Test build
performance" check, bump `kotlinVersion` in
`BuildSpeed/buildSrc/build.gradle.kts` to the Kotlin version the Spine stack
is built with (see `Kotlin.version` in this repo's `buildSrc`), push the
BuildSpeed commit, and update the submodule pointer here.
