---
name: no-gradle-kotlin-dsl-task-delegates
description: >-
  `val x by tasks.registering { }` and `by creating` are deprecated in
  Gradle 9 and go away in Gradle 10 — use `tasks.register("name") { }`;
  verify changed build logic with `--warning-mode all`.
metadata:
  type: feedback
  since: 2026-08-13
---

Do not use the Kotlin DSL property-delegate syntax for task registration —
`val x by tasks.registering { }`, `val x by tasks.creating { }` — in build
scripts. Use an explicit `val x = tasks.register("x") { }` instead. The
delegate form is deprecated in Gradle 9 and is scheduled for removal in
Gradle 10 (see the Gradle 9 upgrading guide, "Kotlin DSL delegated
properties").

**Why:** On 2026-08-13 a freshly written verification task used
`by tasks.registering`; neither the author pass nor the `gradle-review`
agent flagged it — the human did. Gradle 9.6.1 prints the deprecation only
under `--warning-mode all`, so it hides behind the generic "Deprecated
Gradle features were used" banner in default builds.

**How to apply:** When writing or reviewing Gradle Kotlin DSL, treat any
`by registering` / `by creating` as a finding. After touching build logic,
run the affected tasks once with `--warning-mode all` and treat every
deprecation pointing at a changed line as a must-fix. Known leftover not
fixable from consumer repos: `detekt-code-analysis.gradle.kts:67` in the
`config`-distributed `buildSrc` still uses the delegate form and needs an
upstream fix in the `config` repository.
