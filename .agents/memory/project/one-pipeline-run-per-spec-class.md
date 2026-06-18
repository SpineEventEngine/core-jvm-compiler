---
name: one-pipeline-run-per-spec-class
description: A `PluginTestSetup`-based functional spec can call `runPipeline` only once per class; the compiler backend's integration broker is closed after a run, so a second call throws "FilterChain is already closed".
metadata:
  type: project
  since: 2026-06-17
---

In the `*-tests` modules, suites extending `PluginTestSetup` run the Spine
Compiler pipeline via `runPipeline`. The compiler backend creates a
process-wide `io.spine.server` integration broker and **closes it when the
pipeline finishes**. A second `runPipeline` call in the same test class then
fails with `IllegalStateException: FilterChain[...] is already closed` (raised
from `Pipeline.emitEvents`), not the assertion you expect.

**Why:** observed 2026-06-17 (issue #99) — adding a second
`assertThrows<Compilation.Error> { runPipeline(...) }` method to
`IdFieldErrorSpec` made the *first* test fail with "already closed". The
existing suites work because each pipeline-running class calls `runPipeline`
exactly once (most via a single `@BeforeAll`), and the test JVM is fresh per
class.

**How to apply:** put each pipeline-failing scenario in its **own** spec class
(one `runPipeline` per class) — e.g. `UnsupportedIdTypeErrorSpec` sits beside
`IdFieldErrorSpec` rather than adding a method to it. To assert many inputs at
the unit level instead, call the reaction directly (e.g.
`RequiredIdReaction.withField` via a test subclass, as `RequiredIdReactionSpec`
does) — that path has no broker and may be called repeatedly.

Related: [[prototap-build-cache]]
