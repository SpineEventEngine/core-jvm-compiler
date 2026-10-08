---
slug: write-settings-configuration-cache
branch: write-settings-configuration-cache
owner: claude
status: in-review
started: 2026-10-08
related-memories:
  - igtest-stale-plugins
  - gradle-needs-utf8-locale
  - spine-json-needs-task-action-classloader
---

# Make `writeSpineCompilerPluginsSettings` configuration-cache compatible

## Goal

`WriteCompilerPluginsSettings` stores and reuses a configuration-cache entry with
no problems attributed to it or to `CoreJvmCompilerSettings`, while writing
settings files byte-identical to those of the published plugin.

## Context

Follows the merged change that stopped calling `Task.project` in the task action
(version `2.0.0-SNAPSHOT.095`). Remaining blockers:

1. The task holds `options: CoreJvmOptions`; its `compiler` settings and their
   sub-settings hold a `Project`, which the configuration cache cannot serialize
   (confirmed in `money`: "cannot serialize object of type `DefaultProject`").
2. `compilerSettings by lazy { options.compiler!!.toProto() }` runs in the action;
   `CoreJvmCompilerSettings.toProto()` calls `buildClasspath()`, which walks
   `project.tasks.withType(JavaCompile)` and resolves every compile classpath.
3. The task declares an output but no inputs, so Gradle considers it UP-TO-DATE
   after a `coreJvm { }` DSL change — the settings files can go stale.

Finding: nothing in the `summit` repositories reads `Combined.classpath`, nor the
`spine.tools.java.Classpath` type. The settings proto file is `(internal_all)`.
The task writes only the entity, signal, group, UUID, comparable, annotation, and
code-style settings, so the classpath is computed and discarded.

## Plan

- [x] Bump the version to `2.0.0-SNAPSHOT.096` (`bump-version`, no commit).
- [x] `base`: drop `buildClasspath()` from `CoreJvmCompilerSettings.toProto()`;
      mark `Combined.classpath` as `deprecated` in `settings.proto`, documenting
      that it is no longer populated.
- [x] `gradle-plugin`: replace `options`/`compilerSettings` in the task with
      `@get:Input internal abstract val settings: MapProperty<String, Message>` —
      the settings protos keyed by plugin ID; the action renders and writes JSON.
- [x] Obtain the map via `provider { }` in `createWriteSettingsTask()`, using
      the outer `Project`, not `Task.project`.
- [x] Keep `@DisableCachingByDefault`, rewording its reason; inputs now make
      up-to-date checks correct.
- [x] Tests: `CoreJvmCompilerSettingsSpec` (no compile classpath resolution),
      `CoreJvmPluginSpec` (settings type per plugin ID), `CoreJvmPluginIgTest`
      (`--configuration-cache`: entry stored with all settings files written, then
      reused with the task UP-TO-DATE; an options change reruns the task).
- [x] `./gradlew clean build dokkaGenerate --warning-mode all` (incl. `integrationTests`).
- [x] `money` consumer check on a scratch clone: CC store + reuse, `diff -r` of the
      settings against the baseline, deprecations vs. baseline.
- [x] `/pre-pr` reviewers (`spine-code-review`, `kotlin-engineer`, `review-docs`,
      `gradle-review`): all APPROVE WITH CHANGES; findings applied.

## Log

- 2026-10-08 — baseline in `money` with `.094`: 3 CC problems — `gcloud` external
  process (`money` build script), plus `DefaultProject` serialization and
  `Task.project` at execution, both in `WriteCompilerPluginsSettings` (`.094` still
  calls `Task.project` in the action). 7 settings files captured.
- 2026-10-08 — first full build failed: rendering JSON in the provider runs during
  input fingerprinting, where the context class loader is Gradle's, so `KnownTypes`
  cannot find `desc.ref`. Inputs are now the protos; JSON is rendered in the action.
- 2026-10-08 — `money` with `.096`: CC entry stored and reused; the only problem is
  the `gcloud` one; 7 settings files byte-identical (with and without CC); the only
  deprecation change is the removed `Task.project` one. A `coreJvm` DSL change now
  reruns the task (with `.094` it stayed UP-TO-DATE with stale settings).
- 2026-10-08 — final code: clean build green; in `money`, CC entry stored and reused
  across separate JVMs (`--no-daemon`), settings byte-identical. Another branch off
  `master` also published `2.0.0-SNAPSHOT.096` to Maven Local; whichever merges second
  must re-bump.
