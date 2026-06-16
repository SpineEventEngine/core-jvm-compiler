---
slug: file-option-enum-events
branch: raise-coverage
owner: claude
status: done
started: 2026-06-11
---

## Goal

File-wide API options (e.g. `(internal_all) = true`) propagate to enums
declared in the file, so generated Java enums get the corresponding
API-level annotations, as messages and services already do.

## Context

`FileOptionsProcess` emits `FileOptionMatched` events only for entries of
`typeMap` (messages) and `serviceMap`. Enums (`enumTypeMap`) are skipped,
although the event declares an `enum_type` target, `toEnumTypeName()`
exists, and `EnumAnnotationsView` subscribes to the event. As a result
`EnumAnnotator.annotateType` is dead code for file-wide options.

The gap was verified empirically by the in-JVM `PipelineSetup` test in
the `annotation-tests` module: the test
`not yet annotate an enum declared in a file with a file-wide option`
documents the broken behavior.

## Plan

- [x] Confirm `ProtobufSourceFile.enum_type` map and `EnumType.name`
      in the compiler API (`compiler/api`, `source.proto`, `ast.proto`).
- [x] Add `addEnumEvents` to `FileOptionsProcess.kt`, mirroring
      `addMessageEvents`, with `apiOption.messageOption` as `assumed`.
- [x] Set `file = e.file` in `EnumAnnotationsView` handlers (see Log).
- [x] Flip `EnumAnnotatorSpec`: the `Level` enum from
      `internal_all_multiple.proto` must now be annotated `@Internal`.
- [x] Run `:annotation:test` and `:annotation-tests:test`.
- [x] Commit the fix (`4a4559b12`, three files only).
- [x] Full `./gradlew build` for downstream modules (`plugins`
      consumes `:annotation`). The only failure is unrelated to this
      task (see Log).

## Log

- 2026-06-11 — verified event wiring (`EnumAnnotationsView`, routing) and
  the compiler AST shape; only `EnumAnnotatorSpec` asserts the old behavior.
- 2026-06-11 — `addEnumEvents` alone was not enough: the flipped test still
  failed. Root cause: `EnumAnnotations.file` is `(required) = true`, but
  `EnumAnnotationsView` handlers never set `file`, so the view state failed
  validation and was never stored; `ProtoAnnotator.doRender()` then selected
  nothing. Mirrored `MessageAnnotationsView`: set `file = e.file` in both
  handlers, plus the same option-dedup guards. All 23 + 48 tests pass.
- 2026-06-11 — committed as `4a4559b12` (three files only, per request).
- 2026-06-11 — full `./gradlew build --continue`: single failure,
  `ApiAnnotationsPluginIgTest > not annotate > if message option overrides
  file option`. Not caused by this task: a concurrent session edited that
  test (working tree, 18:35) to check top-level classes instead of nested
  ones, exposing the pre-existing "false option does not revert file-wide
  option" gap tracked by `honor-false-api-options.md`. The fixture type is
  a message; this task changed only enum propagation. Everything else,
  including `:annotation-tests:test` and all downstream modules, passed.
