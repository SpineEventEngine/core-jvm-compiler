---
slug: lazy-compiler-config-tasks
branch: claude/hopeful-mccarthy-1dfdf7
owner: claude
status: in-review
started: 2026-10-08
related-memories:
  - no-gradle-kotlin-dsl-task-delegates
  - igtest-stale-plugins
---

## Goal
`Project.configureCompiler()` in `CompilerConfigPlugin.kt` configures tasks
lazily, without realizing tasks eagerly, and fails with a clear message when
the `compiler` options are missing. Task dependencies stay as they are.

## Plan
- [x] `withType<LaunchSpineCompiler>().all { }` → `configureEach { }`.
- [x] `findByName(processResources/sourcesJar)?.mustRunAfter(...)` →
      `tasks.named { <name filter> }.configureEach { }`; still a no-op
      when the task does not exist.
- [x] `coreJvmOptions.compiler!!` → `checkNotNull(...) { "..." }`.
- [x] Bump the version (`bump-version`): `.095` → `.096`.
- [x] `./gradlew build dokkaGenerate --warning-mode all`; no new deprecations.

## Log
- 2026-10-08 — drafted from the user's spec, executing.
- 2026-10-08 — build green incl. `CoreJvmPluginIgTest`; a consumer fixture
  confirmed unchanged `mustRunAfter`/`dependsOn` wiring and zero deprecations
  from the plugin. Applying `io.spine.core-jvm` still realizes all tasks in a
  consumer (0% lazy vs 95% without it) — the cause lies elsewhere in the
  plugin chain; spun off as a separate investigation.
