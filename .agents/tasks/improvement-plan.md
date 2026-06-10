---
slug: improvement-plan
branch: claude/loving-hypatia-tmv0w1
owner: claude
status: draft
started: 2026-06-10
---

## Goal

Close the findings of the 2026-06 repository audit that are owned by
this repository: unify the dual plugin-registration paths, trim the
`api(...)` re-exports of `base`, and add direct tests for modules that
have none. The full audit report (covering this repo and `compiler`)
lives in the `compiler` repository at `docs/audit-2026-06.md`; finding
IDs below (A2, A3, …) refer to it.

## Context

- The audit found no Critical issues; this plan covers the Medium/Low
  items with the highest leverage.
- Tasks marked `[config]` live in files vendored from the
  `SpineEventEngine/config` submodule and must be fixed there, then
  synced — do NOT edit them here.
- Pre-GA is the cheap moment for API-surface work; after 2.0.0 GA the
  `api(...)` trims become breaking changes.

## Plan

Milestone 0 — safety net:

- [ ] (A3 prep) Golden-file characterization tests for
      `WriteCompilerPluginsSettings` output: one expected settings file
      per plugin, so the registry refactoring below cannot silently
      change what is written. Effort M, risk Low.

Milestone 2 — high-leverage:

- [ ] (A3) Introduce a single plugin-registration registry
      `{ pluginClass, settingsId, writeSettings(dir, options) }`
      consumed by both `CompilerConfigPlugin` (currently
      `CompilerConfigPlugin.kt:157-170`) and
      `WriteCompilerPluginsSettings` (currently
      `WriteCompilerPluginsSettings.kt:85-94`), so adding a feature
      plugin is one declaration. Keep the explicit ordering semantics
      (validation first, annotations after features). Gotcha: the style
      writer reads `options.style`, not `compilerSettings` — the
      lambda must accept the options root. Effort M, risk Medium,
      depends on the golden tests above.
- [ ] (A2) Review the `api(...)` re-exports in
      `base/build.gradle.kts:72-76` (`Compiler.api`, `Compiler.jvm`,
      `Validation.context`, `KotlinPoet.lib`): keep only those that
      appear in `base`'s own public signatures, each with a
      `because(...)`; demote the rest to `implementation`. Coordinate
      with the `compiler` repo task narrowing `jvm → backend`.
      Effort M, risk Medium.
- [ ] (T1) Add direct unit specs for `base` (settings round-trips,
      extension defaults) and `grpc`. Kotest assertions, `Spec` suffix,
      stubs not mocks. Effort M, risk Low.

Milestone 3 — polish:

- [x] (Doc2) Fill in `.agents/project.md` (was the unfilled template).
- [ ] (Q2) Replace user-facing `!!` with `checkNotNull` + actionable
      message; start with `WriteCompilerPluginsSettings.kt:80`
      (`options.compiler!!`). Effort S, risk Low.
- [ ] `[config]` Items tracked in the `compiler` repo plan: scrambled
      PAT removal, CI concurrency groups, action SHA-pinning, coverage
      policy note.

## Log

- 2026-06-10 — audit completed; plan drafted together with the filled
  `project.md`; awaiting human review of the audit's open questions
  (audit report, section 6) before execution starts.
