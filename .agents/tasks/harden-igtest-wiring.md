---
slug: harden-igtest-wiring
branch: harden-igtest-wiring
owner: claude
status: in-review
started: 2026-10-19
related-memories:
  - igtest-stale-plugins
---

## Goal

Remove two latent defects in the integration-test wiring that the
`core-jvm-compiler` coverage investigation turned up, so that neither can
surface later as a coverage or staleness bug.

## Context

A sibling investigation checked whether this repo suffers the Kover/JaCoCo
coverage non-determinism diagnosed in `SpineEventEngine/compiler`, where the
`tests` composite build recompiled the root modules between the `test` tasks
and `koverXmlReport`, giving the classes new JaCoCo class IDs so their hits
were discarded. The verdict here was *refuted* — but only accidentally:

1. `tests/settings.gradle.kts` declares `includeBuild("$rootDir/../")` with an
   explicit `dependencySubstitution` block whose single mapping,
   `io.spine.tools:spine-core-jvm` -> `:`, names an artifact no Spine
   repository publishes. Nothing is substituted, so the included build never
   runs a task and never writes into the root project directories. Anyone who
   "corrects" the dead coordinate, or deletes the block so that Gradle's
   automatic substitution resumes, imports the `compiler` defect wholesale.
2. `build.gradle.kts` wires `integrationTests` to the per-module `test` tasks
   with `subprojects.forEach { it.tasks.findByName("test") }`. The hand-off
   claimed this runs while the root project is still configuring, so that
   `findByName` returns `null` and no dependency is added. Measurement refutes
   that: `tasks.register {}` defers the block until the task is realized —
   here, when Gradle builds the task graph, after every subproject has been
   evaluated — so the lookup succeeds and the ordering holds today. It is
   nevertheless fragile: anything that realizes `integrationTests` during
   configuration would run the block early, and the ordering would vanish with
   no error.

The shape of the fix for (1) is `compiler` commit `d2c4fd8a448b` on branch
`deterministic-coverage`: drop `includeBuild`, consume the artifacts under
test from Maven Local, and give Maven Local priority for the coordinates the
root build publishes.

## Plan

- [x] Read the sibling verdict note, `AGENTS.md`, and `.agents/guidelines/`.
- [x] Confirm `localPublish` publishes everything the `tests` build consumes
      from this repo (`core-jvm-gradle-plugin`, `core-jvm-plugins`; the other
      modules are shaded into those two).
- [x] Drop `includeBuild` from `tests/settings.gradle.kts`; document why the
      build must not become a composite again.
- [x] Put a content-filtered `mavenLocal()` ahead of the remote repositories
      for `io.spine.tools:core-jvm-*`, so the just-built artifacts win over an
      already-published version of the same coordinates. Declared directly in
      `tests/build.gradle.kts`, in both the `buildscript` block and
      `allprojects { repositories { .. } }` — a `gradle.beforeProject` hook in
      the settings file cannot work here, because `allprojects` populates the
      subprojects' repositories from the root project's evaluation, before
      their own `beforeProject` callbacks fire, so the hook's repository lands
      last for every subproject.
- [x] Rewire `integrationTests` with a realization-order-safe dependency on
      the per-module `test` tasks.
- [x] Update `tests/README.md` for the new, non-composite arrangement.
- [x] Verify with `./gradlew integrationTests --dry-run` and
      `dependencyInsight`; run a green `clean build`.

## Log

- 2026-10-19 — drafted from the sibling coverage verdict; executing.
- 2026-10-19 — item 1 confirmed and fixed. Item 2 refuted as stated (see
  above) and kept as hardening. `:factories` now resolves
  `io.spine.tools:core-jvm-gradle-plugin:2.0.0-SNAPSHOT.098`, a version that
  exists only in Maven Local, so the integration tests exercise the locally
  built artifacts.
