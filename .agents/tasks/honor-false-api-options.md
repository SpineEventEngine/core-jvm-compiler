---
slug: honor-false-api-options
branch: raise-coverage
owner: claude
status: in-review
started: 2026-06-11
---

## Goal

A message-level `option (internal_type) = false;` reverts a file-wide
`option (internal_all) = true;`, so the generated class is *not* annotated
`@Internal`. More generally: annotators honor the *value* of API-level
options everywhere, instead of treating mere option presence as `true`.

## Context

Verified with an in-JVM `PipelineSetup` test: `Reverting` from
`reverting.proto` *is* annotated despite `(internal_type) = false`.
Root cause: `MessageAnnotationsView` correctly keeps the message-level
`false` option, but `TypeAnnotator.annotate` maps view options through
`ApiOption.findMatching`, which matches by name only. The same
name-only pattern exists in `FieldAnnotator.annotateField` and
`OuterClassAnnotationDiscovery.on(FileEntered)`. Additionally,
`MessageOrEnumAnnotator.needsAnnotation` compares full `Option` messages
(`header.optionList.contains(...)`), which never matches compiler-produced
options (they carry the field number), so the single-file semantic
duplication check is dead code.

The IgTest case "not annotate / if message option overrides file option"
is vacuous: for a `java_multiple_files = true` proto it inspects nested
types of the (empty) outer class.

## Plan

- [x] `TypeAnnotator.annotate`: skip options whose value is not `true`
      (`Option.value.isTrue()` from `ApiOption.kt`).
- [x] `FieldAnnotator.annotateField`: same value filter for field options.
- [x] `OuterClassAnnotationDiscovery`: same value filter for file options.
- [x] `MessageOrEnumAnnotator.needsAnnotation`: match the header option by
      name and `true` value instead of whole-message equality.
- [x] Fixtures (`annotation-tests`): add a `false`-valued field option to
      `reverting.proto`; add single-file `outer_reverting.proto` with
      `(internal_all) = false`.
- [x] Specs: flip and rename the documenting test in `MessageAnnotatorSpec`
      (`shouldNotContain`); add nested-type suppression test (single-file +
      file-wide option ⇒ only the outer class is annotated); add
      `FieldAnnotatorSpec` and `OuterClassAnnotatorSpec` cases for
      `false`-valued options.
- [x] IgTest: make "if message option overrides file option" non-vacuous
      via `checkTypeAnnotations` (the proto is `java_multiple_files = true`).
- [x] Run `:annotation:test` and `:annotation-tests:test`; fix fallout.
  - `:annotation-tests:build` green (26/26) with `--no-build-cache`;
    see the `prototap-build-cache` memory for the cache gotcha.
  - `:annotation:test` green (48/48) after clearing the staleness layers
    described in the `igtest-stale-plugins` memory; the non-vacuous
    IgTest check now passes against the `.072` fat jar.

## Log

- 2026-06-11 — investigation handed over from the previous session;
  plan drafted; executing.
- 2026-06-11 — code changes done; `:annotation-tests` suite green
  (the user flipped the `Reverting` test concurrently in the IDE;
  kept their wording, added the `OrBuilder` assertion).
- 2026-06-11 — first `:annotation:test` run failed only on the
  newly non-vacuous IgTest check: the nested build ran fat jar `.070`
  (stale `writeArtifactMeta` resource + build cache + warm daemons; see
  the `igtest-stale-plugins` memory). After `writeArtifactMeta --rerun`,
  republishing, `--stop`, and clearing the TestKit daemon/cache, the
  manual repro generates `ReveringFileOption` without `@Internal`,
  keeps `InternalMessage` nested types annotated, and annotates only
  the outer class in `OuterInternal` (1 annotation vs 7 before).
