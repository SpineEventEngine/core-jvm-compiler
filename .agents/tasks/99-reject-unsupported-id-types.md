---
slug: 99-reject-unsupported-id-types
branch: claude/jovial-sammet-78b1be
owner: claude
status: in-review
started: 2026-06-17
---

# Provide clean compilation failure on unsupported ID field types

Issue: https://github.com/SpineEventEngine/core-jvm-compiler/issues/99

## Goal

Fail compilation when the ID field (the first declared field of an entity state
or the target-entity ID of a command) has an unsupported type. By convention
(`io.spine.base.Identifier`) an ID must be a `string`, a 32-bit or 64-bit
integer, or a `Message`. It cannot be a `repeated` field, a `map`, or any other
scalar/enum type.

## Context

This is the follow-up to issue #87, which added `checkFieldIsNotEmpty` to the
shared `RequiredIdReaction.withField`. Both implicit-required ID reactions funnel
through `withField`:
- `entity/.../EntityStateIdReaction` — entity-state ID (first field).
- `signal/.../CommandTargetReaction` — command target-entity ID (first field).

So the new type check lives there once (DRY) and covers both.

Supported ID types come from `io.spine.base.Identifier` (String, Integer, Long,
Message). At the proto level: `TYPE_STRING`; all 32/64-bit integer primitives
(`int32/uint32/sint32/fixed32/sfixed32` and the 64-bit variants); and any
message. NOT: `bool`, `float`, `double`, `bytes`, `enum`, `repeated`, `map`.

`RequiredFieldSupport.isSupported` (validation) is a *different* axis
(`!isPrimitive || primitive in {STRING, BYTES}`); integer IDs are valid IDs but
not `(required)`-supported, so they remain `ignore()`d, not errors.

## Plan

- [x] `base/.../field/RequiredIdReaction.kt`
  - Add `checkIdTypeSupported(field, file)` as the first statement of `withField`
    (runs before the explicit-`(required)` early return, so wrong-typed but
    explicitly-required IDs are still rejected).
  - Add private `FieldType.isSupportedIdType()` and the `supportedIdPrimitives` set.
  - Update `withField` KDoc.
- [x] `base/.../field/RequiredIdReactionSpec.kt` (uses `farm.proto`, full matrix)
  - test #2: `rating` (double, now rejected) → `size` (int32: valid ID, not
    req-supported → still `ignore`).
  - test #5: drop `repeated`/`map` cases (now rejected); keep singular messages.
  - Add: reject `repeated` (`tags`,`counts`,`barns`); reject `map`
    (`barns_by_name`,`names_by_id`); reject scalar/enum (`active`,`data`,`rating`,`color`).
- [x] entity end-to-end: `RepeatedIdEntity` fixture in `organization.proto`,
  exclude in `EntityPluginTestSetup`, reject test in a dedicated
  `UnsupportedIdTypeErrorSpec`.
- [x] signal end-to-end: `RepeatedIdCommand` fixture in `commands.proto`,
  exclude in `SignalPluginTestSetup`, reject test in a dedicated
  `UnsupportedCommandIdTypeErrorSpec`.

> Note: each functional test suite runs the pipeline **once** — the compiler
> backend's integration broker is closed after a run. So the end-to-end reject
> tests go in their own classes rather than as extra methods on the existing
> `Empty`-ID error specs.

## Verify

`:base:test :entity-tests:test :signal-tests:test` (JDK 17, `LC_ALL=C.UTF-8`,
output to a log file).

## Log
- 2026-06-17 — drafted plan after mapping the shared `withField` chokepoint and
  the `farm.proto` test matrix; verified `isSupported()` semantics and the
  `PrimitiveType` enum against the compiler/validation sources.
- 2026-06-17 — implemented; `:base:test :entity-tests:test :signal-tests:test`
  green (`--no-build-cache`, JDK 17). `RequiredIdReactionSpec` 8/8;
  `UnsupportedIdTypeErrorSpec` and `UnsupportedCommandIdTypeErrorSpec` 1/1 each.
  CI-equivalent `detekt` green on all three modules.
- 2026-06-17 — review pass (kotlin-engineer: APPROVE; spine-code-review:
  request-changes). Fixes applied: bumped version `.076 -> .077` via
  `bump-version`; corrected 4 doc cross-refs to the new spec classes; KDoc "such
  as"/short-circuit clarity; message "of the type" -> "of type". Re-verified.
  Version bump left uncommitted pending user go-ahead.
- 2026-06-17 — review follow-ups: clarified the error message to list the
  unsupported scalar/enum types; renamed `BrokenIdCommand` -> `EmptyIdCommand`;
  made `FieldType.isSupportedIdType()` `internal @VisibleForTesting` and added
  `SupportedIdTypeSpec` — an enumeration-driven matrix over `PrimitiveType` (with
  a completeness guard that forces classification of any future primitive) plus
  message/enum/repeated/map. Closes the prior gaps (long + all integer variants
  positive; float negative). `:base:test` 8+7 green, `:base:detekt` clean.
- 2026-06-17 — `spine-code-review` loop (2 rounds, broke early on a clean
  round 2). Round 1: fixed one nit (the `Unsupported*ErrorSpec` KDocs now cite
  `SupportedIdTypeSpec` for the matrix); rejected one false-positive (a
  pre-existing "McJava" KDoc line, not in this branch's diff). Final
  `:base/:entity-tests/:signal-tests :test`+`:detekt` all green.
