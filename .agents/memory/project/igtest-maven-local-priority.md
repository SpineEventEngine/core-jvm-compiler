---
name: igtest-maven-local-priority
description: The `tests` build must declare the filtered `mavenLocal()` first in `tests/build.gradle.kts`; a `gradle.beforeProject` hook in `tests/settings.gradle.kts` lands last for every subproject.
metadata:
  type: project
  since: 2026-10-19
---

The `tests` build consumes `io.spine.tools:core-jvm-gradle-plugin` and
`io.spine.tools:core-jvm-plugins` from Maven Local, where the root
`localPublish` task installs them. Maven Local has to come **first**, because
`standardToSpineSdk()` appends `mavenLocal()` *after* the remote Spine
repositories — so once the version under development is published, a remote
repository serves it and the integration tests silently exercise the earlier
artifacts. The declarations live in `tests/build.gradle.kts`: one in the
`buildscript` block (the plugin classpath) and one in
`allprojects { repositories { .. } }`, each a `mavenLocal()` restricted with
`content { includeModuleByRegex("io\\.spine\\.tools", "core-jvm-.*") }`.

**Why:** `SpineEventEngine/compiler` solves the same problem from
`settings.gradle.kts` with `gradle.beforeProject { .. }` (commit
`d2c4fd8a448b`). Copying that shape here does not work. `allprojects` runs
during the *root* project's evaluation, which precedes every subproject's own
`beforeProject` callback, so the hook's repository is appended **after** the
ones `allprojects` already declared. Measured with an init script printing
`repositories.map { it.name }` from `gradle.projectsEvaluated`: the injected
repository came first for `:` but eleventh for `:factories`.
`RepositoryHandler` offers no prepend operation.

**How to apply:** when changing repository declarations in `tests/`, keep the
filtered `mavenLocal()` as the first entry in both places, and verify with the
init-script probe rather than by reading the scripts. Confirm the result with
`cd tests && ./gradlew :factories:dependencyInsight --configuration
compileClasspath --dependency core-jvm-gradle-plugin` — it must report the
version from the working tree's `version.gradle.kts`. Related:
[[igtest-stale-plugins]].
